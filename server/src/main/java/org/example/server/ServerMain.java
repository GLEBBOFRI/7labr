package org.example.server;

import org.example.authentication.UserManager;
import org.example.database.DatabaseManager;
import org.example.database.CollectionManager;
import org.example.network.Request;
import org.example.network.Response;
import org.example.server.commands.Command;
import org.example.server.commands.*;
import org.example.database.exceptions.ValidationException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class ServerMain {
    private static final Logger logger = Logger.getLogger(ServerMain.class.getName());
    private final int port;
    private final CollectionManager collectionManager;
    private final Map<String, Command> commands;
    private volatile boolean isRunning;
    private final ForkJoinPool commandExecutionPool;
    private final ExecutorService responseSendingPool;
    private final ReentrantLock collectionLock = new ReentrantLock();
    private final UserManager userManager;

    private static class ClientState {
        ByteBuffer headerBuffer = ByteBuffer.allocate(4);
        ByteBuffer dataBuffer = null;
        boolean readingHeader = true;

        ByteBuffer responseBufferToSend = null;
        Response responseToSend = null;

        private final ReentrantLock channelWriteLock = new ReentrantLock();

        public void reset() {
            headerBuffer.clear();
            readingHeader = true;
            dataBuffer = null;
            responseBufferToSend = null;
            responseToSend = null;
        }

        public ReentrantLock getChannelWriteLock() {
            return channelWriteLock;
        }
    }

    public ServerMain(int port, CollectionManager collectionManager, Map<String, Command> commands, UserManager userManager) {
        this.port = port;
        this.collectionManager = collectionManager;
        this.commands = commands;
        this.userManager = userManager;
        this.isRunning = true;
        this.commandExecutionPool = new ForkJoinPool();
        this.responseSendingPool = Executors.newCachedThreadPool();
        setupLogger();
    }

    private void setupLogger() {
        try {
            FileHandler fileHandler = new FileHandler("server.log", true);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
            logger.addHandler(fileHandler);
            logger.setLevel(Level.INFO);
        } catch (IOException e) {
            System.err.println("Ошибка настройки логгера: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            DatabaseManager databaseManager = new DatabaseManager();
            UserManager userManager = new UserManager(databaseManager);
            CollectionManager collectionManager = new CollectionManager(databaseManager);

            Map<String, Command> commands = new HashMap<>();
            registerCommands(commands, collectionManager, userManager);

            new ServerMain(12345, collectionManager, commands, userManager).start();
        } catch (SQLException e) {
            System.err.println("Не удалось запустить сервер: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Неожиданная ошибка при запуске сервера: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void start() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {

            serverChannel.bind(new InetSocketAddress("0.0.0.0", port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Сервер запущен на порту " + port);

            while (isRunning) {
                selector.select(500);
                if (!isRunning) {
                    break;
                }

                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        acceptConnection(key, selector);
                    } else if (key.isReadable()) {
                        readRequest(key);
                    } else if (key.isWritable()) {
                        ClientState clientState = (ClientState) key.attachment();
                        if (clientState != null && clientState.responseBufferToSend != null) {
                            responseSendingPool.submit(() -> {
                                try {
                                    sendResponse(
                                            (SocketChannel) key.channel(), key, clientState, clientState.responseToSend);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                        }
                    }
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                logger.log(Level.SEVERE, "Ошибка сервера: " + e.getMessage(), e);
            }
        } finally {
            shutdownServer();
            if (collectionManager != null) {
                collectionManager.closeDatabaseConnection();
            }
            logger.info("Сервер остановлен");
        }
    }

    private void acceptConnection(SelectionKey key, Selector selector) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ, new ClientState());
            logger.info("Клиент подключен: " + clientChannel.getRemoteAddress());
        }
    }

    private void readRequest(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        ClientState clientState = (ClientState) key.attachment();

        try {
            if (clientState.readingHeader) {
                int bytesRead = clientChannel.read(clientState.headerBuffer);
                if (bytesRead == -1) {
                    logger.info("Клиент " + clientChannel.getRemoteAddress() + " корректно отключился.");
                    clientChannel.close();
                    return;
                }
                if (clientState.headerBuffer.hasRemaining()) {
                    return;
                }
                clientState.headerBuffer.flip();
                int requestLength = clientState.headerBuffer.getInt();

                if (requestLength <= 0 || requestLength > 10 * 1024 * 1024) {
                    logger.warning("Получена неверная или чрезмерная длина запроса (" + requestLength + ") от " + clientChannel.getRemoteAddress() + ". Закрываем соединение.");
                    clientChannel.close();
                    return;
                }
                clientState.dataBuffer = ByteBuffer.allocate(requestLength);
                clientState.readingHeader = false;
            }

            int bytesRead = clientChannel.read(clientState.dataBuffer);
            if (bytesRead == -1) {
                logger.info("Клиент " + clientChannel.getRemoteAddress() + " отключился во время чтения данных.");
                clientChannel.close();
                return;
            }
            if (clientState.dataBuffer.hasRemaining()) {
                return;
            }

            clientState.dataBuffer.flip();
            byte[] requestBytes = new byte[clientState.dataBuffer.remaining()];
            clientState.dataBuffer.get(requestBytes);

            Request request = deserializeRequest(requestBytes);
            logger.info("Получен запрос от " + clientChannel.getRemoteAddress() + ": " + request.getCommandName());

            key.interestOps(0);

            commandExecutionPool.submit(() -> {
                Response response;
                String authenticatedUsername = null;

                String username = request.getUsername();
                String password = request.getPassword();

                if ("register".equalsIgnoreCase(request.getCommandName())) {
                    if (username != null && password != null && !username.isEmpty() && !password.isEmpty()) {
                        if (userManager.registerUser(username, password)) {
                            response = new Response("Успешная регистрация. Вы вошли в систему как " + username + ".");
                            authenticatedUsername = username;
                        } else {
                            response = new Response("Ошибка регистрации: Пользователь с таким именем уже существует или внутренняя ошибка.");
                        }
                    } else {
                        response = new Response("Ошибка: Для регистрации требуются имя пользователя и пароль.");
                    }
                } else if ("login".equalsIgnoreCase(request.getCommandName())) {
                    if (username != null && password != null && !username.isEmpty() && !password.isEmpty()) {
                        if (userManager.authenticateUser(username, password)) {
                            response = new Response("Успешный вход. Добро пожаловать, " + username + "!");
                            authenticatedUsername = username;
                        } else {
                            response = new Response("Ошибка входа: Неверное имя пользователя или пароль.");
                        }
                    } else {
                        response = new Response("Ошибка: Для входа требуются имя пользователя и пароль.");
                    }
                } else {
                    if (username == null || password == null || username.isEmpty() || password.isEmpty() || !userManager.authenticateUser(username, password)) {
                        response = new Response("Ошибка: Для выполнения этой команды необходима аутентификация. Пожалуйста, используйте 'register' или 'login'.");
                    } else {
                        authenticatedUsername = username;
                        try {
                            collectionLock.lock();
                            Command command = commands.get(request.getCommandName().toLowerCase());
                            if (command == null) {
                                response = new Response("Команда не найдена.");
                                logger.warning("Команда не найдена от " + clientChannel.getRemoteAddress() + ": " + request.getCommandName());
                            } else {
                                response = command.execute(request, authenticatedUsername);
                                logger.info("Выполнена команда '" + request.getCommandName() + "' для " + authenticatedUsername + " (" + clientChannel.getRemoteAddress() + ").");
                            }
                        } catch (Exception e) {
                            response = new Response("Ошибка при выполнении команды: " + e.getMessage());
                            try {
                                logger.log(Level.SEVERE, "Ошибка при выполнении команды для " + authenticatedUsername + " (" + clientChannel.getRemoteAddress() + "): " + e.getMessage(), e);
                            } catch (IOException ex) {
                                throw new RuntimeException(ex);
                            }
                        } finally {
                            collectionLock.unlock();
                        }
                    }
                }
                try {
                    sendResponse(clientChannel, key, clientState, response);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

        } catch (IOException e) {
            logger.log(Level.WARNING, "Ошибка чтения запроса от клиента " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
            try { clientChannel.close(); } catch (IOException ex) { logger.log(Level.SEVERE, "Ошибка закрытия канала: " + ex.getMessage()); }
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Класс не найден во время десериализации запроса от клиента " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
            try { clientChannel.close(); } catch (IOException ex) { logger.log(Level.SEVERE, "Ошибка закрытия канала: " + ex.getMessage()); }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Неожиданная ошибка в readRequest для " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
            try { clientChannel.close(); } catch (IOException ex) { logger.log(Level.SEVERE, "Ошибка закрытия канала: " + ex.getMessage()); }
        }
    }

    private void sendResponse(SocketChannel clientChannel, SelectionKey key, ClientState clientState, Response response) throws IOException {
        clientState.getChannelWriteLock().lock();
        try {
            clientState.responseBufferToSend = ByteBuffer.wrap(serializeResponse(response));

            while (clientState.responseBufferToSend.hasRemaining()) {
                int bytesWritten = clientChannel.write(clientState.responseBufferToSend);
                if (bytesWritten == 0) {
                    TimeUnit.MILLISECONDS.sleep(1);
                }
            }
            logger.info("Ответ отправлен клиенту " + clientChannel.getRemoteAddress());

            clientState.reset();
            key.selector().wakeup();
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            logger.log(Level.WARNING, "Ошибка отправки ответа клиенту " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
            try { clientChannel.close(); } catch (IOException ex) { logger.log(Level.SEVERE, "Ошибка закрытия канала: " + ex.getMessage()); }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Неожиданная ошибка в sendResponse для " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
            try { clientChannel.close(); } catch (IOException ex) { logger.log(Level.SEVERE, "Ошибка закрытия канала: " + ex.getMessage()); }
        } finally {
            clientState.getChannelWriteLock().unlock();
        }
    }

    private void shutdownServer() {
        try {
            commandExecutionPool.shutdown();
            responseSendingPool.shutdown();
            if (!commandExecutionPool.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Пул выполнения команд не завершился вовремя, принудительное завершение.");
                commandExecutionPool.shutdownNow();
            }
            if (!responseSendingPool.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warning("Пул отправки ответов не завершился вовремя, принудительное завершение.");
                responseSendingPool.shutdownNow();
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Ошибка при завершении пулов потоков: " + e.getMessage(), e);
        }
    }

    private static void registerCommands(Map<String, Command> commands,
                                         CollectionManager collectionManager,
                                         UserManager userManager) {
        commands.put("register", new RegisterCommand(userManager));
        commands.put("login", new LoginCommand(userManager));

        commands.put("info", new Info(collectionManager));
        commands.put("show", new Show(collectionManager));
        commands.put("insert", new Insert(collectionManager));
        commands.put("update", new Update(collectionManager));
        commands.put("remove_key", new RemoveKey(collectionManager));
        commands.put("clear", new Clear(collectionManager));
        commands.put("replace_if_greater", new ReplaceIfGreater(collectionManager));
        commands.put("remove_greater_key", new RemoveGreaterKey(collectionManager));
        commands.put("remove_lower_key", new RemoveLowerKey(collectionManager));
        commands.put("remove_all_by_standard_of_living",
                new RemoveAllByStandardOfLiving(collectionManager));
        commands.put("average_of_meters_above_sea_level",
                new AverageOfMetersAboveSeaLevel(collectionManager));
        commands.put("filter_starts_with_name",
                new FilterStartsWithName(collectionManager));
    }

    private static byte[] serializeResponse(Response response) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(response);
            byte[] objectBytes = bos.toByteArray();

            ByteBuffer buffer = ByteBuffer.allocate(4 + objectBytes.length);
            buffer.putInt(objectBytes.length);
            buffer.put(objectBytes);
            return buffer.array();
        }
    }

    private static Request deserializeRequest(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Request) ois.readObject();
        }
    }
}
