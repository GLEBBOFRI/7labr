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
import java.io.Serializable; // важно для Request/Response
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
import java.util.concurrent.TimeUnit;
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
            serverChannel.configureBlocking(false); // сам серверный канал неблокирующий
            serverChannel.register(selector, SelectionKey.OP_ACCEPT);

            logger.info("сервер запущен на порту " + port);

            while (isRunning) {
                selector.select(500); // неблокирующая операция ждет до 500 мс событий
                if (!isRunning) { // проверка, не был ли сервер остановлен
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
                        // Удален блок if (key.isReadable()), так как клиентские каналы
                        // теперь обрабатываются в блокирующем режиме в отдельных потоках
                        // и не регистрируются с этим селектором для чтения.
                    }
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                logger.log(Level.SEVERE, "ошибка сервера: " + e.getMessage(), e);
            }
        } finally {
            try {
                executorService.shutdown(); // останавливаем пул потоков
                // ждем завершения всех задач, до 10 секунд
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    logger.warning("исполнитель не завершил работу вовремя, принудительное завершение.");
                    executorService.shutdownNow();
                }
                collectionManager.shutdownSaveExecutor(); // теперь здесь, при полном завершении сервера
            } catch (Exception e) {
                logger.log(Level.SEVERE, "ошибка при завершении исполнителя или менеджера коллекции: " + e.getMessage(), e);
            }
            logger.info("сервер остановлен");
        }
    }

    private void accept(ServerSocketChannel serverChannel, Selector selector) throws IOException {
        SocketChannel clientChannel = serverChannel.accept();
        if (clientChannel != null) {
            // Важно: clientChannel по умолчанию будет неблокирующим, так как ServerSocketChannel неблокирующий.
            // Мы НЕ регистрируем его с этим селектором для OP_READ.
            // clientChannel.configureBlocking(false); // Эту строку удаляем, она избыточна или может вызвать проблемы
            // clientChannel.register(selector, SelectionKey.OP_READ); // Эту строку удаляем!

            logger.info("клиент подключен: " + clientChannel.getRemoteAddress());
            // Передаем новый клиентский канал сразу в пул потоков для обработки
            executorService.submit(() -> {
                try {
                    handleClient(clientChannel);
                } catch (Exception e) { // ловим все исключения, чтобы поток не упал
                    try {
                        logger.log(Level.SEVERE, "ошибка в потоке обработки клиента для " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                    try {
                        clientChannel.close(); // закрываем канал при ошибке
                    } catch (IOException ex) {
                        logger.log(Level.SEVERE, "ошибка закрытия канала после исключения: " + ex.getMessage(), ex);
                    }
                }
            });
        }
    }

    /**
     * обрабатывает запросы от одного клиента.
     * этот метод читает запросы, обрабатывает их и отправляет ответы,
     * используя протокол "длина + данные".
     *
     * @param clientChannel канал клиента, с которым нужно работать.
     */
    private void handleClient(SocketChannel clientChannel) throws IOException {
        try {
            // делаем канал блокирующим для операций чтения/записи в этом потоке.
            // это сильно упрощает логику, так как read() и write() будут ждать завершения.
            // Эту строку теперь безопасно вызвать здесь, так как канал не зарегистрирован с Selector.
            clientChannel.configureBlocking(true);

            // цикл для обработки нескольких запросов от одного клиента
            while (true) {
                // 1. читаем 4-байтовый префикс длины запроса
                ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
                int totalBytesReadLength = 0;
                while (totalBytesReadLength < 4) {
                    int bytes = clientChannel.read(lengthBuffer);
                    if (bytes == -1) {
                        logger.info("клиент " + clientChannel.getRemoteAddress() + " корректно отключился.");
                        return; // клиент отключился
                    }
                    // в блокирующем режиме read() должен блокировать, пока не будут доступны данные.
                    // если bytes == 0, это может быть очень короткий таймаут или специфическое поведение системы.
                    // для надежности, мы продолжаем цикл.
                    if (bytes == 0) {
                        continue;
                    }
                    totalBytesReadLength += bytes;
                }
                lengthBuffer.flip(); // готовим буфер длины к чтению
                int requestLength = lengthBuffer.getInt(); // получаем ожидаемую длину запроса

                // базовая проверка длины запроса, чтобы избежать ошибок памяти или вредоносных данных.
                // предполагаем разумный максимальный размер запроса (например, 10 мб).
                if (requestLength <= 0 || requestLength > 10 * 1024 * 1024) {
                    logger.warning("получена неверная или чрезмерная длина запроса (" + requestLength + ") от " + clientChannel.getRemoteAddress() + ". закрываем соединение.");
                    return; // закрываем соединение из-за неверной длины
                }

                // 2. читаем фактические данные запроса
                ByteBuffer requestDataBuffer = ByteBuffer.allocate(requestLength);
                int totalBytesReadData = 0;
                while (totalBytesReadData < requestLength) {
                    int bytes = clientChannel.read(requestDataBuffer);
                    if (bytes == -1) {
                        logger.info("клиент " + clientChannel.getRemoteAddress() + " отключился во время чтения данных.");
                        return; // клиент отключился
                    }
                    // аналогично, в блокирующем режиме это не должно происходить.
                    if (bytes == 0) {
                        continue;
                    }
                    totalBytesReadData += bytes;
                }
                requestDataBuffer.flip(); // готовим буфер данных к чтению

                byte[] requestBytes = new byte[requestDataBuffer.remaining()];
                requestDataBuffer.get(requestBytes);

                Request request = deserializeRequest(requestBytes);

                logger.info("получен запрос от " + clientChannel.getRemoteAddress() + ": " + request.getCommandName());
                Command command = commands.get(request.getCommandName().toLowerCase());

                Response response;
                if (command == null) {
                    response = new Response("команда не найдена");
                    logger.warning("команда не найдена от " + clientChannel.getRemoteAddress() + ": " + request.getCommandName());
                } else {
                    response = command.execute(request);
                    logger.info("выполнена команда '" + request.getCommandName() + "' для " + clientChannel.getRemoteAddress() + ", отправляем ответ.");
                }

                // 3. отправляем ответ (с префиксом длины)
                byte[] responseBytes = serializeResponse(response);
                ByteBuffer responseSendBuffer = ByteBuffer.wrap(responseBytes);
                while (responseSendBuffer.hasRemaining()) { // гарантируем полную отправку
                    clientChannel.write(responseSendBuffer);
                }
            }
        } catch (IOException e) {
            logger.log(Level.WARNING, "ошибка при обработке клиента " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "класс не найден во время десериализации для клиента " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
        } catch (Exception e) { // ловим любые другие неожиданные исключения
            logger.log(Level.SEVERE, "неожиданная ошибка в handleClient для " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
        } finally {
            try {
                if (clientChannel.isOpen()) {
                    clientChannel.close();
                    logger.log(Level.INFO, "сокет закрыт для " + clientChannel.getRemoteAddress());
                }
            } catch (IOException e) {
                logger.log(Level.SEVERE, "ошибка закрытия клиентского сокета для " + clientChannel.getRemoteAddress() + ": " + e.getMessage(), e);
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

    /**
     * сериализует объект ответа в массив байтов, добавляя в начало его длину (4 байта).
     * это важно для сетевого протокола, чтобы клиент знал, сколько байтов нужно прочитать.
     * @param response объект ответа для сериализации.
     * @return массив байтов, содержащий 4-байтовый префикс длины, за которым следует сериализованный объект ответа.
     * @throws IOException если произошла ошибка ввода-вывода во время сериализации.
     */
    private static byte[] serializeResponse(Response response) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(response);
            byte[] objectBytes = bos.toByteArray();

            // создаем новый буфер для хранения длины и данных
            ByteBuffer buffer = ByteBuffer.allocate(4 + objectBytes.length);
            buffer.putInt(objectBytes.length); // записываем длину объекта (4 байта)
            buffer.put(objectBytes);           // затем сами байты объекта
            return buffer.array();             // возвращаем полный массив байтов
        }
    }

    /**
     * десериализует массив байтов обратно в объект запроса.
     * этот метод предполагает, что входной массив байтов содержит только сериализованные данные объекта запроса (без префикса длины).
     * префикс длины обрабатывается логикой чтения сети в handleClient.
     * @param data массив байтов, содержащий сериализованный объект запроса.
     * @return десериализованный объект запроса.
     * @throws IOException если произошла ошибка ввода-вывода во время десериализации.
     * @throws ClassNotFoundException если класс десериализованного объекта не может быть найден.
     */
    private static Request deserializeRequest(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Request) ois.readObject();
        }
    }
}