package org.example;

import org.example.comands.Command;
import org.example.comands.*;
import org.example.network.Request;
import org.example.network.Response;
import org.example.consol.Console;
import org.example.consol.StandartConsole;
import org.example.exceptions.CommandExecutionError;
import org.example.exceptions.CommandNotFoundException;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

public class ClientMain {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 12345;
    private static final long RECONNECTION_DELAY_MS = 300;
    private static final int MAX_RECONNECTION_ATTEMPTS = 5;
    private static final int BUFFER_SIZE = 8192; // этот размер буфера теперь в основном для начального чтения, а не для фиксированного размера сообщения

    private final Console console = new StandartConsole();
    private final CommandManager commandManager = new CommandManager();
    private SocketChannel socketChannel;

    public static void main(String[] args) {
        new ClientMain().run();
    }

    private void initializeCommands() {
        commandManager.registerCommand(new Help(console, commandManager));
        commandManager.registerCommand(new Exit(console));
        commandManager.registerCommand(new ExecuteScript(console, commandManager));

        commandManager.registerCommand(new ServerCommand("info", "показывает инфо о коллекциях"));
        commandManager.registerCommand(new ServerCommand("show", "показывает все элементы коллекции"));
        commandManager.registerCommand(new ServerCommand("insert", "добавляет новый элемент"));
        commandManager.registerCommand(new ServerCommand(
                "update", "обновляет элемент по айди"));
        commandManager.registerCommand(new ServerCommand("remove_key", "удаляет элемент по айди"));
        commandManager.registerCommand(new ServerCommand("clear", "очищает коллекцию"));
        commandManager.registerCommand(new ServerCommand("replace_if_greater", "замена если больше"));
        commandManager.registerCommand(new ServerCommand("remove_greater_key", "удаляет элементы с айди больше чем"));
        commandManager.registerCommand(new ServerCommand("remove_lower_key", "удаляет элементы с айди меньше чем"));
        commandManager.registerCommand(new ServerCommand("remove_all_by_standard_of_living",
                "удаляет все города с этими стандартами проживания"));
        commandManager.registerCommand(new ServerCommand("average_of_meters_above_sea_level",
                "считает среднее значение высоты над уровнем моря"));
        commandManager.registerCommand(new ServerCommand("filter_starts_with_name",
                "фильтрует элементы по названию"));
    }

    private class ServerCommand extends Command {
        public ServerCommand(String name, String description) {
            super(name, description);
        }

        @Override
        public void execute(String[] args) throws CommandExecutionError {
            try {
                Request request = new Request(this.getName(), args);
                Response response = sendRequest(request);
                console.writeln(response.getMessage());
                if (response.getData() != null) {
                    console.writeln(response.getData().toString());
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new CommandExecutionError("Error sending request to server: " + e.getMessage());
            }
        }
    }

    /**
     * отправляет запрос на сервер и получает ответ.
     * этот метод теперь обрабатывает фрагментированные сетевые сообщения, сначала отправляя/читая длину сообщения,
     * затем отправляя/читая фактическое содержимое сообщения в цикле, пока все байты не будут переданы.
     *
     * @param request объект запроса для отправки.
     * @return объект ответа, полученный от сервера.
     * @throws IOException если произошла ошибка сети или ввода-вывода.
     * @throws ClassNotFoundException если класс ответа не может быть найден во время десериализации.
     */
    public Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        if (socketChannel == null || !socketChannel.isConnected()) {
            throw new IOException("Not connected to the server.");
        }

        // этап 1: отправка запроса
        // сериализуем объект запроса, включая его длину как 4-байтовый префикс.
        byte[] requestBytes = serialize(request);
        ByteBuffer sendBuffer = ByteBuffer.wrap(requestBytes);

        // убеждаемся, что все байты запроса отправлены, даже если это потребует нескольких вызовов записи.
        while (sendBuffer.hasRemaining()) {
            socketChannel.write(sendBuffer);
        }
        sendBuffer.clear(); // очищаем буфер отправки для возможного повторного использования

        // этап 2: получение ответа
        // 1. сначала читаем 4 байта, чтобы узнать ожидаемую длину входящего ответа.
        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        while (lengthBuffer.hasRemaining()) {
            int bytes = socketChannel.read(lengthBuffer);
            if (bytes == -1) {
                throw new IOException("Server disconnected while reading response length.");
            }
        }
        lengthBuffer.flip(); // готовим буфер длины к чтению
        int responseLength = lengthBuffer.getInt(); // получаем ожидаемую длину ответа

        // базовая проверка длины ответа, чтобы избежать ошибок памяти или вредоносных данных.
        // предполагаем разумный максимальный размер ответа (например, 10 мб).
        if (responseLength <= 0 || responseLength > 10 * 1024 * 1024) {
            throw new IOException("Invalid or excessive response length received: " + responseLength);
        }

        // 2. теперь читаем само содержимое ответа, пока не получим все ожидаемые байты.
        ByteBuffer responseBuffer = ByteBuffer.allocate(responseLength);
        int totalBytesRead = 0;
        while (totalBytesRead < responseLength) {
            int bytes = socketChannel.read(responseBuffer);
            if (bytes == -1) {
                throw new IOException("Server disconnected while reading response data.");
            }
            totalBytesRead += bytes;
        }

        responseBuffer.flip(); // готовим буфер ответа к чтению

        // необязательно: проверяем, что количество прочитанных данных соответствует ожидаемой длине.
        if (responseBuffer.remaining() != responseLength) {
            throw new IOException("Incomplete response: expected " + responseLength + " bytes, but read " + responseBuffer.remaining());
        }

        byte[] responseBytes = new byte[responseBuffer.remaining()];
        responseBuffer.get(responseBytes);

        return deserialize(responseBytes);
    }

    private void connect() throws IOException {
        int attempts = 0;
        while (attempts < MAX_RECONNECTION_ATTEMPTS) {
            try {
                socketChannel = SocketChannel.open();
                socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
                console.writeln("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
                return; // Successful connection, exit method
            } catch (IOException e) {
                attempts++;
                console.writeln("Failed to connect to server (attempt " + attempts + "/" + MAX_RECONNECTION_ATTEMPTS + "): " + e.getMessage());
                if (attempts >= MAX_RECONNECTION_ATTEMPTS) {
                    throw new IOException("Failed to connect to server after " + MAX_RECONNECTION_ATTEMPTS + " attempts.", e);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(RECONNECTION_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Connection interrupted.", ie);
                }
            }
        }
        throw new IOException("Failed to connect after max attempts");
    }

    private void disconnect() {
        try {
            if (socketChannel != null) {
                socketChannel.close();
                socketChannel = null;
                console.writeln("Disconnected from server.");
            }
        } catch (IOException e) {
            console.writeln("Error closing connection: " + e.getMessage());
        }
    }

    public void run() {
        initializeCommands();

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                try {
                    connect();
                    break;
                } catch (IOException e) {
                    console.writeln(e.getMessage());
                }
            }

            console.writeln("Type 'help' for command list");

            while (true) {
                try {
                    console.write("> ");
                    String input = console.read().trim();
                    if (input.isEmpty()) continue;

                    String[] parts = input.split("\\s+", 2);
                    String commandName = parts[0];
                    String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

                    Command command = commandManager.getCommand(commandName);
                    command.execute(args);

                    if ("exit".equalsIgnoreCase(commandName)) {
                        break;
                    }
                } catch (CommandNotFoundException e) {
                    console.writeln("Error: Command not found. Type 'help' for available commands");
                } catch (CommandExecutionError e) {
                    console.writeln("Error: " + e.getMessage());
                    if (e.getCause() instanceof IOException) {
                        console.writeln("Connection to server lost. Attempting to reconnect...");
                        disconnect();
                        try{
                            connect();
                            console.writeln("Reconnected to server. Please re-enter the last command.");
                            continue;
                        } catch (IOException ex){
                            console.writeln(ex.getMessage());
                            break;
                        }

                    }
                }
            }
        } finally {
            disconnect();
            console.writeln("Client stopped");
        }
    }

    /**
     * сериализует объект в массив байтов, добавляя в начало его длину (4 байта).
     * это очень важно для сетевого протокола, чтобы знать, сколько байтов нужно прочитать.
     * @param obj объект для сериализации.
     * @return массив байтов, содержащий 4-байтовый префикс длины, за которым следует сериализованный объект.
     * @throws IOException если произошла ошибка ввода-вывода во время сериализации.
     */
    private static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            byte[] objectBytes = bos.toByteArray();

            // создаем новый буфер для хранения длины и данных
            ByteBuffer buffer = ByteBuffer.allocate(4 + objectBytes.length);
            buffer.putInt(objectBytes.length); // записываем длину объекта (4 байта)
            buffer.put(objectBytes);           // затем сами байты объекта
            return buffer.array();             // возвращаем полный массив байтов
        }
    }

    /**
     * десериализует массив байтов обратно в объект.
     * этот метод предполагает, что входной массив байтов содержит только сериализованные данные объекта (без префикса длины).
     * префикс длины обрабатывается логикой чтения сети в sendRequest.
     * @param data массив байтов, содержащий сериализованный объект.
     * @return десериализованный объект.
     * @throws IOException если произошла ошибка ввода-вывода во время десериализации.
     * @throws ClassNotFoundException если класс десериализованного объекта не может быть найден.
     */
    private static <T> T deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (T) ois.readObject();
        }
    }
}
