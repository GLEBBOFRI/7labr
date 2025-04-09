package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.collection.models.City;
import org.example.collection.models.Coordinates;
import org.example.client.exceptions.CommandExecutionError;

public class ReplaceIfGreater extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public ReplaceIfGreater(Console console, CollectionManager collectionManager) {
        super("replace_if_greater", "заменить значение по ключу, если новое значение больше старого");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 1) {
            throw new CommandExecutionError("смотри брат эта команда с 1 аргументом просто напиши replace_if_greater <ключ>");
        }

        try {
            Integer key = Integer.parseInt(arguments[0]);
            City oldCity = collectionManager.getCollection().get(key);

            if (oldCity == null) {
                throw new CommandExecutionError("элемент с ключом " + key + " не найден.");
            }

            // создание нового объекта City
            console.writeln("введи данные для нового города:");
            City newCity = new City();

            console.writeln("введи название города:");
            newCity.setName(readNonEmptyString("Название города не может быть пустым."));

            console.writeln("введи координату X (целое число, больше -81):");
            Integer x = readIntegerGreaterThan(-81, "Координата X должна быть целым числом больше -81.");
            console.writeln("введи координату Y (целое число):");
            Long y = readLong("Координата Y должна быть целым числом.");
            newCity.setCoordinates(new Coordinates(x, y));

            console.writeln("введи площадь города (целое число, больше 0):");
            newCity.setArea(readIntegerGreaterThan(0, "Площадь должна быть целым числом больше 0."));

            console.writeln("введи население города (целое число, больше 0):");
            newCity.setPopulation(readLongGreaterThan(0, "Население должно быть целым числом больше 0."));

            console.writeln("введи высоту над уровнем моря (число с плавающей точкой):");
            newCity.setMetersAboveSeaLevel(readFloat("Высота над уровнем моря должна быть числом."));

            // сравниваем
            if (isNewCityGreater(newCity, oldCity)) {
                collectionManager.add(key, newCity);
                console.writeln("элемент заменён.");
            } else {
                console.writeln("новое значение не больше старого.");
            }

            return true;
        } catch (NumberFormatException e) {
            throw new CommandExecutionError("неправильный формат ключа. Ключ должен быть целым числом.");
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при замене элемента: " + e.getMessage());
        }
    }

    // проверяем больше ор но
    private boolean isNewCityGreater(City newCity, City oldCity) {
        if (newCity.getPopulation() == null || oldCity.getPopulation() == null) {
            throw new IllegalArgumentException("невозможно сравнить города: одно из полей population равно null.");
        }
        return newCity.getPopulation().compareTo(oldCity.getPopulation()) > 0;
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

    private Integer readIntegerGreaterThan(int minValue, String errorMessage) {
        while (true) {
            try {
                String input = console.read();
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

    private Long readLong(String errorMessage) {
        while (true) {
            try {
                String input = console.read();
                return Long.parseLong(input);
            } catch (NumberFormatException e) {
                console.writeln(errorMessage);
            }
        }
    }

    private Long readLongGreaterThan(long minValue, String errorMessage) {
        while (true) {
            try {
                String input = console.read();
                long value = Long.parseLong(input);
                if (value > minValue) {
                    return value;
                }
                console.writeln(errorMessage);
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
}
