package org.example;

import org.example.comands.Command;
import org.example.comands.*;
import org.example.exceptions.CommandExecutionError;
import org.example.exceptions.CommandNotFoundException;
import org.example.network.Request;
import org.example.network.Response;
import org.example.consol.Console;
import org.example.consol.StandartConsole;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.InputMismatchException;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.ArrayList;
import java.util.List;

public class ClientMain {
    private static String SERVER_HOST = "localhost";
    private static int SERVER_PORT = 12345;

    private static final long RECONNECTION_DELAY_MS = 300;
    private static final int MAX_RECONNECTION_ATTEMPTS = 5;

    private final Console console;
    private final CommandManager commandManager;
    private SocketChannel socketChannel;

    private String currentUsername = null;
    private String currentPassword = null;

    public ClientMain() {
        this.console = new StandartConsole();
        this.commandManager = new CommandManager();
    }

    public static void main(String[] args) {
        if (args.length >= 1) {
            SERVER_HOST = args[0];
            if (args.length >= 2) {
                try {
                    SERVER_PORT = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Некорректный порт: " + args[1] + ". Используется порт по умолчанию: " + SERVER_PORT);
                }
            }
        } else {
            SERVER_HOST = "server";
        }

        ClientMain client = new ClientMain();
        client.run();
    }

    private void initializeCommands() {
        commandManager.registerCommand(new Help(console, commandManager));
        commandManager.registerCommand(new Exit(console));
        commandManager.registerCommand(new ExecuteScript(console, commandManager));

        commandManager.registerCommand(new ClientAuthCommand("register", "регистрирует нового пользователя"));
        commandManager.registerCommand(new ClientAuthCommand("login", "выполняет вход в систему"));

        commandManager.registerCommand(new ServerCommand("info", "показывает инфо о коллекциях"));
        commandManager.registerCommand(new ServerCommand("show", "показывает все элементы коллекции"));
        commandManager.registerCommand(new ServerCommand("insert", "добавляет новый элемент"));
        commandManager.registerCommand(new ServerCommand("update", "обновляет элемент по айди"));
        commandManager.registerCommand(new ServerCommand("remove_key", "удаляет элемент по айди"));
        commandManager.registerCommand(new ServerCommand("clear", "очищает коллекцию (удаляет только ваши города)"));
        commandManager.registerCommand(new ServerCommand("replace_if_greater", "замена если больше (для ваших городов)"));
        commandManager.registerCommand(new ServerCommand("remove_greater_key", "удаляет элементы с айди больше чем (удаляет только ваши города)"));
        commandManager.registerCommand(new ServerCommand("remove_lower_key", "удаляет элементы с айди меньше чем (удаляет только ваши города)"));
        commandManager.registerCommand(new ServerCommand("remove_all_by_standard_of_living", "удаляет все города с этими стандартами проживания (удаляет только ваши города)"));
        commandManager.registerCommand(new ServerCommand("average_of_meters_above_sea_level", "считает среднее значение высоты над уровнем моря"));
        commandManager.registerCommand(new ServerCommand("filter_starts_with_name", "фильтрует элементы по названию"));
    }

    private class ServerCommand extends Command {
        public ServerCommand(String name, String description) {
            super(name, description);
        }

        @Override
        public void execute(String[] args) throws CommandExecutionError {
            if (!"register".equalsIgnoreCase(getName()) && !"login".equalsIgnoreCase(getName())) {
                if (currentUsername == null || currentPassword == null) {
                    console.writeln("Ошибка: Для выполнения этой команды необходима аутентификация. Пожалуйста, используйте 'register' или 'login'.");
                    return;
                }
            }

            try {
                Request request = new Request(this.getName(), args, currentUsername, currentPassword);
                Response response = sendRequest(request);
                console.writeln(response.getMessage());
                if (response.getData() != null) {
                    console.writeln(response.getData().toString());
                }
            } catch (IOException | ClassNotFoundException e) {
                throw new CommandExecutionError("Ошибка при отправке запроса на сервер: " + e.getMessage());
            }
        }
    }

    private class ClientAuthCommand extends Command {
        public ClientAuthCommand(String name, String description) {
            super(name, description);
        }

        @Override
        public void execute(String[] args) throws CommandExecutionError {
            if (args.length != 0) {
                console.writeln("Ошибка: Команда '" + getName() + "' не принимает аргументов напрямую.");
                console.writeln("Использование: " + getName());
                return;
            }

            String usernameInput;
            String passwordInput;

            console.write("Введите имя пользователя: ");
            usernameInput = console.read().trim();
            if (usernameInput.isEmpty()) {
                throw new CommandExecutionError("Имя пользователя не может быть пустым.");
            }

            console.write("Введите пароль: ");
            passwordInput = console.read().trim();
            if (passwordInput.isEmpty()) {
                throw new CommandExecutionError("Пароль не может быть пустым.");
            }

            currentUsername = usernameInput;
            currentPassword = passwordInput;

            try {
                new ServerCommand(this.getName(), "").execute(null);
            } catch (CommandExecutionError e) {
                currentUsername = null;
                currentPassword = null;
                throw e;
            }
        }
    }

    public Response sendRequest(Request request) throws IOException, ClassNotFoundException {
        if (socketChannel == null || !socketChannel.isConnected()) {
            throw new IOException("Не подключено к серверу. Попытка переподключения...");
        }

        byte[] requestBytes = serialize(request);
        ByteBuffer sendBuffer = ByteBuffer.wrap(requestBytes);

        while (sendBuffer.hasRemaining()) {
            socketChannel.write(sendBuffer);
        }
        sendBuffer.clear();

        ByteBuffer lengthBuffer = ByteBuffer.allocate(4);
        int totalBytesReadLength = 0;
        while (totalBytesReadLength < 4) {
            int bytes = socketChannel.read(lengthBuffer);
            if (bytes == -1) {
                throw new IOException("Сервер отключился во время чтения длины ответа.");
            }
            if (bytes == 0) {
                continue;
            }
            totalBytesReadLength += bytes;
        }
        lengthBuffer.flip();
        int responseLength = lengthBuffer.getInt();

        if (responseLength <= 0 || responseLength > 10 * 1024 * 1024) {
            throw new IOException("Получена неверная или чрезмерная длина ответа: " + responseLength);
        }

        ByteBuffer responseBuffer = ByteBuffer.allocate(responseLength);
        int totalBytesReadData = 0;
        while (totalBytesReadData < responseLength) {
            int bytes = socketChannel.read(responseBuffer);
            if (bytes == -1) {
                throw new IOException("Сервер отключился во время чтения данных ответа.");
            }
            if (bytes == 0) {
                continue;
            }
            totalBytesReadData += bytes;
        }

        responseBuffer.flip();

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
                console.writeln("Подключено к серверу на " + SERVER_HOST + ":" + SERVER_PORT);
                return;
            } catch (IOException e) {
                attempts++;
                console.writeln("Не удалось подключиться к серверу (попытка " + attempts + "/" + MAX_RECONNECTION_ATTEMPTS + "): " + e.getMessage());
                if (attempts >= MAX_RECONNECTION_ATTEMPTS) {
                    throw new IOException("Не удалось подключиться к серверу после " + MAX_RECONNECTION_ATTEMPTS + " попыток.", e);
                }
                try {
                    TimeUnit.MILLISECONDS.sleep(RECONNECTION_DELAY_MS);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Подключение прервано.", ie);
                }
            }
        }
        throw new IOException("Не удалось подключиться после максимального количества попыток.");
    }

    private void disconnect() {
        try {
            if (socketChannel != null) {
                socketChannel.close();
                socketChannel = null;
                console.writeln("Отключено от сервера.");
            }
        } catch (IOException e) {
            console.writeln("Ошибка при закрытии соединения: " + e.getMessage());
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

            console.writeln("Введите 'help' для списка команд.");
            console.writeln("Для начала работы зарегистрируйтесь ('register') или войдите ('login').");

            while (true) {
                try {
                    console.write("> ");
                    String input = console.read().trim();
                    if (input.isEmpty()) continue;

                    String[] parts = input.split("\\s+", 2);
                    String commandName = parts[0].toLowerCase();
                    String[] initialArgs = parts.length > 1 ? parseArguments(parts[1]) : new String[0];

                    if ("insert".equals(commandName) || "update".equals(commandName) || "replace_if_greater".equals(commandName)) {
                        List<String> collectedArgs = new ArrayList<>();

                        if (("update".equals(commandName) || "replace_if_greater".equals(commandName))) {
                            if (initialArgs.length == 0) {
                                console.writeln("Ошибка: Для команды '" + commandName + "' требуется ключ (ID).");
                                continue;
                            }
                            try {
                                Integer.parseInt(initialArgs[0]);
                            } catch (NumberFormatException e) {
                                console.writeln("Ошибка: Ключ должен быть целым числом.");
                                continue;
                            }
                            collectedArgs.add(initialArgs[0]);
                        }

                        List<String> cityFields = readCityFieldsFromConsole(console);
                        if (cityFields == null) {
                            console.writeln("Ввод города отменен.");
                            continue;
                        }
                        collectedArgs.addAll(cityFields);

                        new ServerCommand(commandName, "").execute(collectedArgs.toArray(new String[0]));

                    } else {
                        Command command = commandManager.getCommand(commandName);
                        command.execute(initialArgs);
                    }

                    if ("exit".equalsIgnoreCase(commandName)) {
                        break;
                    }
                } catch (CommandNotFoundException e) {
                    console.writeln("Ошибка: Команда не найдена. Введите 'help' для доступных команд.");
                } catch (CommandExecutionError e) {
                    console.writeln("Ошибка: " + e.getMessage());
                    if (e.getCause() instanceof IOException) {
                        console.writeln("Соединение с сервером потеряно. Попытка переподключения...");
                        disconnect();
                        try {
                            connect();
                            console.writeln("Переподключено к серверу. Пожалуйста, повторите последнюю команду.");
                            continue;
                        } catch (IOException ex) {
                            console.writeln(ex.getMessage());
                            break;
                        }
                    }
                } catch (InputMismatchException e) {
                    console.writeln("Неверный тип ввода. Пожалуйста, введите корректное значение.");
                } catch (Exception e) {
                    console.writeln("Произошла неожиданная ошибка: " + e.getMessage());
                }
            }
        } finally {
            disconnect();
            console.writeln("Клиент остановлен.");
        }
    }

    private String[] parseArguments(String argsString) {
        return argsString.split("\\s+");
    }

    private List<String> readCityFieldsFromConsole(Console console) {
        List<String> fields = new ArrayList<>();
        try {
            console.write("Введите название города (String, не пустое): ");
            fields.add(readNonEmptyString(console));

            console.write("Введите координату X (Integer, > -81): ");
            fields.add(readIntegerAsString(console, -80, Integer.MAX_VALUE));

            console.write("Введите координату Y (Long, не null): ");
            fields.add(readLongAsString(console));

            console.write("Введите площадь (Integer, > 0): ");
            fields.add(readIntegerAsString(console, 1, Integer.MAX_VALUE));

            console.write("Введите население (Long, > 0): ");
            fields.add(readLongAsString(console, 1L, Long.MAX_VALUE));

            console.write("Введите высоту над уровнем моря (Float, можно null): ");
            fields.add(readFloatOrNullAsString(console));

            console.writeln("Выберите климат (1-5):");
            console.writeln("    1 - RAIN_FOREST,\n" +
                    "    2 - HUMIDSUBTROPICAL,\n" +
                    "    3 - HUMIDCONTINENTAL,\n" +
                    "    4 - TUNDRA,\n" +
                    "    5 - POLAR_ICECAP");
            fields.add(String.valueOf(readEnumChoice(console, 5)));

            console.writeln("Выберите правительство (1-4, можно null):");
            console.writeln("    1 - DESPOTISM,\n" +
                    "    2 - NOOCRACY,\n" +
                    "    3 - TECHNOCRACY,\n" +
                    "    4 - TIMOCRACY");
            fields.add(String.valueOf(readEnumChoice(console, 4, true)));

            console.writeln("Выберите уровень жизни (1-5):");
            console.writeln("    1 - ULTRA_HIGH,\n" +
                    "    2 - VERY_HIGH,\n" +
                    "3 - LOW,\n" +
                    "4 - VERY_LOW,\n" +
                    "5 - ULTRA_LOW");
            fields.add(String.valueOf(readEnumChoice(console, 5)));

            console.write("Введите имя губернатора (String, можно null): ");
            fields.add(readStringOrNull(console));

            return fields;
        } catch (Exception e) {
            console.writeln("Ошибка при вводе данных города: " + e.getMessage());
            return null;
        }
    }

    private String readNonEmptyString(Console console) {
        String input;
        while (true) {
            input = console.read().trim();
            if (!input.isEmpty()) {
                return input;
            }
            console.writeln("Поле не может быть пустым. Повторите ввод:");
        }
    }

    private String readStringOrNull(Console console) {
        String input = console.read().trim();
        return input.isEmpty() ? "null" : input;
    }

    private String readIntegerAsString(Console console, int min, int max) {
        while (true) {
            try {
                String input = console.read().trim();
                int value = Integer.parseInt(input);
                if (value >= min && value <= max) {
                    return input;
                } else {
                    console.writeln("Значение должно быть в диапазоне от " + min + " до " + max + ". Повторите ввод:");
                }
            } catch (NumberFormatException e) {
                console.writeln("Неверный формат числа. Повторите ввод:");
            }
        }
    }

    private String readLongAsString(Console console) {
        while (true) {
            try {
                return console.read().trim();
            } catch (NumberFormatException e) {
                console.writeln("Неверный формат числа. Повторите ввод:");
            }
        }
    }

    private String readLongAsString(Console console, long min, long max) {
        while (true) {
            try {
                String input = console.read().trim();
                long value = Long.parseLong(input);
                if (value >= min && value <= max) {
                    return input;
                } else {
                    console.writeln("Значение должно быть в диапазоне от " + min + " до " + max + ". Повторите ввод:");
                }
            } catch (NumberFormatException e) {
                console.writeln("Неверный формат числа. Повторите ввод:");
            }
        }
    }

    private String readFloatOrNullAsString(Console console) {
        String input = console.read().trim();
        if (input.isEmpty()) {
            return "null";
        }
        while (true) {
            try {
                Float.parseFloat(input);
                return input;
            } catch (NumberFormatException e) {
                console.writeln("Неверный формат числа (Float). Повторите ввод или оставьте пустым для null:");
                input = console.read().trim();
                if (input.isEmpty()) return "null";
            }
        }
    }

    private int readEnumChoice(Console console, int maxChoice) {
        while (true) {
            try {
                int choice = Integer.parseInt(console.read().trim());
                if (choice >= 1 && choice <= maxChoice) {
                    return choice;
                } else {
                    console.writeln("Неверный выбор. Пожалуйста, введите число от 1 до " + maxChoice + ":");
                }
            } catch (NumberFormatException e) {
                console.writeln("Неверный формат. Пожалуйста, введите число:");
            }
        }
    }

    private int readEnumChoice(Console console, int maxChoice, boolean nullable) {
        while (true) {
            String input = console.read().trim();
            if (nullable && input.isEmpty()) {
                return 0;
            }
            try {
                int choice = Integer.parseInt(input);
                if (choice >= 1 && choice <= maxChoice) {
                    return choice;
                } else {
                    console.writeln("Неверный выбор. Пожалуйста, введите число от 1 до " + maxChoice + ":");
                }
            } catch (NumberFormatException e) {
                console.writeln("Неверный формат. Пожалуйста, введите число или оставьте пустым для null:");
            }
        }
    }

    private static byte[] serialize(Object obj) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(obj);
            byte[] objectBytes = bos.toByteArray();

            ByteBuffer buffer = ByteBuffer.allocate(4 + objectBytes.length);
            buffer.putInt(objectBytes.length);
            buffer.put(objectBytes);
            return buffer.array();
        }
    }

    private static <T> T deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bis)) {
            return (T) ois.readObject();
        }
    }
}
