package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.collection.exceptions.ValidationException;
import org.example.collection.models.*;
import org.example.client.exceptions.CommandExecutionError;
import org.example.collection.DumpManager;
import java.util.Set;

public class Insert extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public Insert(Console console, CollectionManager collectionManager) {
        super("insert", "добавить новый элемент с автоматически сгенерированным ключом");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        try {
            Set<Integer> existingIds = collectionManager.getKeys();
            Integer key = DumpManager.getSmallestAvailableId(existingIds);
            console.writeln("использован сгенерированный ID: " + key);

            // Режим создания города из аргументов (9 параметров)
            if (arguments.length == 9) {
                return createCityFromArguments(key, arguments);
            }

            // Стандартный интерактивный режим
            return createCityInteractive(key);
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при добавлении элемента: " + e.getMessage());
        }
    }

    private boolean createCityFromArguments(Integer key, String[] arguments) throws CommandExecutionError {
        try {
            City city = new City();
            city.setName(arguments[0]);

            // Обработка координат
            Integer x = Integer.parseInt(arguments[1]);
            Long y = Long.parseLong(arguments[2]);
            if (x <= -81) {
                throw new CommandExecutionError("координата X должна быть больше -81");
            }
            city.setCoordinates(new Coordinates(x, y));

            // Обработка площади
            int area = Integer.parseInt(arguments[3]);
            if (area <= 0) {
                throw new CommandExecutionError("площадь должна быть больше 0");
            }
            city.setArea(area);

            // Обработка населения
            long population = Long.parseLong(arguments[4]);
            if (population <= 0) {
                throw new CommandExecutionError("население должно быть больше 0");
            }
            city.setPopulation(population);

            // Обработка высоты над уровнем моря
            city.setMetersAboveSeaLevel(Float.parseFloat(arguments[5]));

            // Обработка климата
            city.setClimate(Climate.valueOf(arguments[6].toUpperCase()));

            // Обработка правительства
            city.setGovernment(Government.valueOf(arguments[7].toUpperCase()));

            // Обработка уровня жизни
            city.setStandardOfLiving(StandardOfLiving.valueOf(arguments[8].toUpperCase()));

            collectionManager.add(key, city);
            console.writeln("Город успешно добавлен из аргументов. Чтобы сохранить изменения, напишите 'save'.");
            return true;
        } catch (IllegalArgumentException e) {
            throw new CommandExecutionError("Неверный формат аргументов: " + e.getMessage());
        } catch (ValidationException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean createCityInteractive(Integer key) throws CommandExecutionError, ValidationException {
        console.writeln("Введите данные для нового города:");
        City city = new City();

        console.writeln("Введите название города:");
        city.setName(readNonEmptyString("Название города не может быть пустым."));

        console.writeln("Введите координату X (целое число, больше -81):");
        Integer x = readIntegerWithLengthCheck(-81, 10, "Координата X должна быть целым числом больше -81 и содержать не более 10 цифр.");

        console.writeln("Введите координату Y (целое число):");
        Long y = readLongWithLengthCheck(10, "Координата Y должна быть целым числом и содержать не более 10 цифр.");
        city.setCoordinates(new Coordinates(x, y));

        console.writeln("Введите площадь города (целое число, больше 0):");
        city.setArea(readIntegerWithLengthCheck(0, 10, "Площадь должна быть целым числом больше 0 и содержать не более 10 цифр."));

        console.writeln("Введите население города (целое число, больше 0):");
        city.setPopulation(readLongWithLengthCheck(10, "Население должно быть целым числом больше 0 и содержать не более 10 цифр."));

        console.writeln("Введите высоту над уровнем моря (число с плавающей точкой):");
        city.setMetersAboveSeaLevel(readFloat("Высота над уровнем моря должна быть числом."));

        city.setClimate(readClimate());
        city.setGovernment(readGovernment());
        city.setStandardOfLiving(readStandardOfLiving());

        console.writeln("Введите имя губернатора (оставьте пустым, если губернатора нет):");
        String governorName = console.read();
        if (!governorName.isEmpty()) {
            city.setGovernor(new Human(governorName));
        }

        collectionManager.add(key, city);
        console.writeln("Город успешно добавлен. Чтобы сохранить изменения, напишите 'save'.");
        return true;
    }

    private String readNonEmptyString(String errorMessage) {
        while (true) {
            String input = console.read();
            if (input != null && !input.isEmpty()) {
                return input;
            }
            console.writeln(errorMessage);
        }
    }

    private Integer readIntegerWithLengthCheck(int minValue, int maxDigits, String errorMessage) {
        while (true) {
            try {
                String input = console.read();
                if (input.length() > maxDigits) {
                    console.writeln("Ошибка: максимальное количество цифр - " + maxDigits + ".");
                    continue;
                }
                int value = Integer.parseInt(input);
                if (value > minValue) {
                    return value;
                }
                console.writeln(errorMessage);
            } catch (NumberFormatException e) {
                console.writeln(errorMessage);
            }
        }
    }

    private Long readLongWithLengthCheck(int maxDigits, String errorMessage) {
        while (true) {
            try {
                String input = console.read();
                if (input.length() > maxDigits) {
                    console.writeln("Ошибка: максимальное количество цифр - " + maxDigits + ".");
                    continue;
                }
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                console.writeln(errorMessage);
            }
        }
    }

    private Float readFloat(String errorMessage) {
        while (true) {
            try {
                String input = console.read();
                return Float.parseFloat(input);
            } catch (NumberFormatException e) {
                console.writeln(errorMessage);
            }
        }
    }

    private Climate readClimate() {
        console.writeln("Выберите климат:");
        for (Climate climate : Climate.values()) {
            console.writeln((climate.ordinal() + 1) + ": " + climate);
        }
        while (true) {
            try {
                int choice = Integer.parseInt(console.read());
                if (choice >= 1 && choice <= Climate.values().length) {
                    return Climate.values()[choice - 1];
                }
                console.writeln("Неправильный выбор. Попробуйте снова.");
            } catch (NumberFormatException e) {
                console.writeln("Введите число.");
            }
        }
    }

    private Government readGovernment() {
        console.writeln("Выберите тип правительства:");
        for (Government government : Government.values()) {
            console.writeln((government.ordinal() + 1) + ": " + government);
        }
        while (true) {
            try {
                int choice = Integer.parseInt(console.read());
                if (choice >= 1 && choice <= Government.values().length) {
                    return Government.values()[choice - 1];
                }
                console.writeln("Неправильный выбор. Попробуйте снова.");
            } catch (NumberFormatException e) {
                console.writeln("Введите число.");
            }
        }
    }

    private StandardOfLiving readStandardOfLiving() {
        console.writeln("Выберите уровень жизни:");
        for (StandardOfLiving standard : StandardOfLiving.values()) {
            console.writeln((standard.ordinal() + 1) + ": " + standard);
        }
        while (true) {
            try {
                int choice = Integer.parseInt(console.read());
                if (choice >= 1 && choice <= StandardOfLiving.values().length) {
                    return StandardOfLiving.values()[choice - 1];
                }
                console.writeln("Неправильный выбор. Попробуйте снова.");
            } catch (NumberFormatException e) {
                console.writeln("Введите число.");
            }
        }
    }
}