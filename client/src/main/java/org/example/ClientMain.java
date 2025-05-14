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
    private static final int BUFFER_SIZE = 8192;

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
        commandManager.registerCommand(new ServerCommand("update", "обновляет элемент по айди"));
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

    private Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        if (socketChannel == null || !socketChannel.isConnected()) {
            throw new IOException("Not connected to the server.");
        }

        // сериализуем запрос в байты
        byte[] requestBytes = serialize(request);
        ByteBuffer buffer = ByteBuffer.wrap(requestBytes);
        socketChannel.write(buffer);
        buffer.clear();

        // получаем и читаем ответ
        ByteBuffer responseBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        int bytesRead = socketChannel.read(responseBuffer);
        if (bytesRead == -1) {
            throw new IOException("Server disconnected.");
        }
        responseBuffer.flip();
        byte[] responseBytes = new byte[responseBuffer.remaining()];
        responseBuffer.get(responseBytes);
        Response response = deserialize(responseBytes);
        return response;
    }

    private void connect() throws IOException {
        int attempts = 0;
        while (attempts < MAX_RECONNECTION_ATTEMPTS) {
            try {
                socketChannel = SocketChannel.open();
                socketChannel.connect(new InetSocketAddress(SERVER_HOST, SERVER_PORT));
                console.writeln("Connected to server at " + SERVER_HOST + ":" + SERVER_PORT);
                return; // Успешное подключение, выходим из метода
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
        throw new IOException("Failed to connect after max attempts"); //Если дошли сюда и не вышли из цикла - ошибка
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
                    break; // выход из бесконечного цикла при успешном подключении
                } catch (IOException e) {
                    console.writeln(e.getMessage()); // сообщение об ошибке подключения
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
    private static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            return bos.toByteArray();
        }
    }

    private static Response deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (Response) ois.readObject();
        }
    }
}
