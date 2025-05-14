package org.example.server;

import org.example.collection.CollectionManager;
import org.example.collection.exceptions.ValidationException;
import org.example.network.Request;
import org.example.network.Response;
import org.example.server.commands.*;

import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
    private final ExecutorService executorService; // пул потоков для обработки клиентов

    public ServerMain(int port, CollectionManager collectionManager, Map<String, Command> commands) {
        this.port = port;
        this.collectionManager = collectionManager;
        this.commands = commands;
        this.isRunning = true;
        this.executorService = Executors.newFixedThreadPool(10); // размер пула потоков
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
            System.err.println("Error setting up logger: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java -jar server.jar <json_file>");
            System.exit(1);
        }

        try {
            CollectionManager collectionManager = new CollectionManager();
            collectionManager.loadCollection(args[0]);

            Map<String, Command> commands = new HashMap<>();
            registerCommands(commands, collectionManager);

            new ServerMain(12345, collectionManager, commands).start();
        } catch (IOException | ValidationException e) {
            System.err.println("Failed to start server: " + e.getMessage());
            System.exit(1);
        }
    }

    public void start() {
        try (ServerSocketChannel serverChannel = ServerSocketChannel.open();
             Selector selector = Selector.open()) {
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.configureBlocking(false);
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("Server started on port " + port);

            while (isRunning) {
                selector.select(500); // неблокирующая операция ждет до 500 мс ака 50 сек событий
                if (!isRunning) { // проверка не был ли сервер остановлен
                    break;
                }
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isValid()) {
                        if (key.isAcceptable()) {
                            accept(serverChannel, selector);
                        }
                        if (key.isReadable()) {
                            SocketChannel clientChannel = (SocketChannel) key.channel();
                            // если все норм то передаем задачу в пул потоков
                            executorService.submit(() -> {
                                try {
                                    handleClient(clientChannel);
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
                logger.log(Level.SEVERE, "Server error: " + e.getMessage(), e);
            }
        } finally {
            try {
                executorService.shutdown(); //стоппим пул потоков
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error shutting down executor: " + e.getMessage(), e);
            }
            logger.info("Server stopped");
        }
    }


    private void accept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel != null) {
            clientChannel.configureBlocking(false);
            clientChannel.register(selector, SelectionKey.OP_READ);
            logger.info("Client connected: " + clientChannel.getRemoteAddress());
        }
    }

    private void handleClient(SocketChannel clientChannel) throws IOException {
        ByteBuffer buffer = ByteBuffer.allocate(8192); // Размер буфера
        try {
            //ObjectInputStream objectInputStream = new ObjectInputStream(clientChannel.socket().getInputStream());
            //ObjectOutputStream objectOutputStream = new ObjectOutputStream(clientChannel.socket().getOutputStream());
            Request request;
            while (true) {
                int bytesRead = clientChannel.read(buffer);
                if (bytesRead == -1) {
                    logger.info("Client " + clientChannel.getRemoteAddress() + " disconnected.");
                    break;
                }
                if (bytesRead > 0) {
                    buffer.flip();
                    byte[] data = new byte[buffer.remaining()];
                    buffer.get(data);
                    request = deserializeRequest(data);
                    buffer.clear(); //после чтения буфер очищаем

                    if (request != null) {
                        logger.info("Received request from " + clientChannel.getRemoteAddress() + ": " + request);
                        Command command = commands.get(request.getCommandName().toLowerCase());

                        Response response;
                        if (command == null) {
                            response = new Response("Command not found");
                            logger.warning("Command not found from " + clientChannel.getRemoteAddress() + ": " + request.getCommandName());
                        } else {
                            response = command.execute(request);
                            logger.info("Executed command '" + request.getCommandName() + "' for " + clientChannel.getRemoteAddress() + ", sent response: " + response);
                        }
                        byte[] responseBytes = serializeResponse(response);
                        ByteBuffer responseBuffer = ByteBuffer.wrap(responseBytes);
                        while (responseBuffer.hasRemaining()) {
                            clientChannel.write(responseBuffer);
                        }
                    }
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "Error handling client " + clientChannel.getRemoteAddress() + ": " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                clientChannel.close();
                logger.log(Level.INFO, "Socket closed for " + clientChannel.getRemoteAddress());
                collectionManager.shutdownSaveExecutor();
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Error closing client socket for " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
                collectionManager.shutdownSaveExecutor();
            }
        }
    }

    private static void registerCommands(Map<String, Command> commands,
                                         CollectionManager collectionManager) {
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
            return bos.toByteArray();
        }
    }

    private static Request deserializeRequest(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Request) ois.readObject();
        }
    }
}
