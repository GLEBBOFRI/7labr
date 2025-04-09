package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.collection.models.*;
import org.example.client.exceptions.CommandExecutionError;

public class Update extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public Update(Console console, CollectionManager collectionManager) {
        super("update", "обновить значение элемента коллекции по ключу");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 1) {
            throw new CommandExecutionError("смотри брат эта команда с 1 аргументом просто напиши update <айди>");
        }

        try {
            // пампим(парсим) нефть (ключ)
            Integer key = Integer.parseInt(arguments[0]);

            // проверка существования
            if (!collectionManager.getKeys().contains(key)) {
                throw new CommandExecutionError("элемент с ключом " + key + " не найден");
            }

            console.writeln("введи новые данные для города:");
            City city = new City();

            // вводим данныи с использованием методов
            console.writeln("введи название города:");
            city.setName(readNonEmptyString("название города не может быть пустым."));
            console.writeln("введи координату X (целое число, больше -81):");
            Integer x = readIntegerWithLengthCheck(-81, 10, "координата X должна быть целым числом больше -81 и содержать не более 10 цифр.");
            console.writeln("введи координату Y (целое число):");
            Long y = readLongWithLengthCheck(10, "координата Y должна быть целым числом и содержать не более 10 цифр.");
            city.setCoordinates(new Coordinates(x, y));
            console.writeln("введи площадь города (целое число, больше 0):");
            city.setArea(readIntegerWithLengthCheck(0, 10, "площадь должна быть целым числом больше 0 и содержать не более 10 цифр."));
            console.writeln("введи население города (целое число, больше 0):");
            city.setPopulation(readLongWithLengthCheck(10, "население должно быть целым числом больше 0 и содержать не более 10 цифр."));
            console.writeln("введи высоту над уровнем моря (число с плавающей точкой):");
            city.setMetersAboveSeaLevel(readFloat("высота над уровнем моря должна быть числом."));
            city.setClimate(readClimate());
            city.setGovernment(readGovernment());
            city.setStandardOfLiving(readStandardOfLiving());
            console.writeln("введи имя губернатора (оставь пустым, если губернатора нет):");
            String governorName = console.read();
            if (!governorName.isEmpty()) {
                city.setGovernor(new Human(governorName));
            }

            // обновляем объект только в оперативной памяти
            collectionManager.update(key, city);
            console.writeln("город успешно обновлен чтобы сохранить изменения, напиши save.");
            return true;
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при обновлении элемента: " + e.getMessage());
        }
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
                    console.writeln("фатальная ошибка: не гони сбавь скорость до " + maxDigits + " цифр.");
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
                    console.writeln("фатальная ошибка: не гони сбавь скорость до " + maxDigits + " цифр.");
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
        console.writeln("выбери климат:");
        for (Climate climate : Climate.values()) {
            console.writeln((climate.ordinal() + 1) + ": " + climate);
        }
        while (true) {
            try {
                int choice = Integer.parseInt(console.read());
                if (choice >= 1 && choice <= Climate.values().length) {
                    return Climate.values()[choice - 1];
                }
                console.writeln("не тот путь брат попробуй сначала");
            } catch (NumberFormatException e) {
                console.writeln("введи число.");
            }
        }
    }

    private Government readGovernment() {
        console.writeln("выбери тип правительства:");
        for (Government government : Government.values()) {
            console.writeln((government.ordinal() + 1) + ": " + government);
        }
        while (true) {
            try {
                int choice = Integer.parseInt(console.read());
                if (choice >= 1 && choice <= Government.values().length) {
                    return Government.values()[choice - 1];
                }
                console.writeln("не тот путь брат попробуй сначала");
            } catch (NumberFormatException e) {
                console.writeln("введи число.");
            }
        }
    }

    private StandardOfLiving readStandardOfLiving() {
        console.writeln("выбери уровень жизни:");
        for (StandardOfLiving standard : StandardOfLiving.values()) {
            console.writeln((standard.ordinal() + 1) + ": " + standard);
        }
        while (true) {
            try {
                int choice = Integer.parseInt(console.read());
                if (choice >= 1 && choice <= StandardOfLiving.values().length) {
                    return StandardOfLiving.values()[choice - 1];
                }
                console.writeln("не тот путь брат попробуй сначала");
            } catch (NumberFormatException e) {
                console.writeln("введи число.");
            }
        }
    }
}
