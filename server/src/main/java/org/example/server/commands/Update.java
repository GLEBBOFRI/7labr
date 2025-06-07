package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;
import org.example.database.models.*;
import org.example.database.exceptions.ValidationException;
import java.sql.SQLException;

public class Update extends Command {
    private final CollectionManager collectionManager;

    public Update(CollectionManager collectionManager) {
        super("update", "обновить элемент по заданному ключу, получая все новые данные в одной строке (для ваших городов). Использование: update <ключ> <название> <x> <y> <площадь> <население> <высота> <код_климата> <код_правительства> <код_уровня_жизни> [<имя_губернатора>]");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) {
        if (authenticatedUsername == null) {
            return new Response("Ошибка: Для выполнения команды 'update' требуется аутентификация. Пожалуйста, используйте 'register' или 'login'.");
        }

        String[] args = (String[]) request.getArguments(); // Теперь аргументы - это String[]
        if (args == null || args.length < 10 || args.length > 11) {
            return new Response("Ты что-то напутал с аргументами, нужно: ключ, название, x, y, площадь, население, высота, код_климата, код_правительства, код_уровня_жизни [, имя_губернатора].");
        }

        try {
            Integer key = Integer.parseInt(args[0]);

            String name = args[1];
            Integer coordinateX = Integer.parseInt(args[2]);
            Long coordinateY = Long.parseLong(args[3]);
            Integer area = Integer.parseInt(args[4]);
            Long population = Long.parseLong(args[5]);
            Float metersAboveSeaLevel = "null".equalsIgnoreCase(args[6]) ? null : Float.parseFloat(args[6]);

            int climateCode = Integer.parseInt(args[7]);
            if (climateCode < 1 || climateCode > Climate.values().length) {
                return new Response("С климатом опять беда, коды от 1 до " + Climate.values().length + ":\n" +
                        "    1 - RAIN_FOREST,\n" +
                        "    2 - HUMIDSUBTROPICAL,\n" +
                        "    3 - HUMIDCONTINENTAL,\n" +
                        "    4 - TUNDRA,\n" +
                        "    5 - POLAR_ICECAP.");
            }
            Climate climate = Climate.values()[climateCode - 1];

            Government government = null;
            if (!"null".equalsIgnoreCase(args[8])) {
                int governmentCode = Integer.parseInt(args[8]);
                if (governmentCode >= 1 && governmentCode <= Government.values().length) {
                    government = Government.values()[governmentCode - 1];
                } else {
                    return new Response("И с правительством тоже, коды от 1 до " + Government.values().length + ":\n" +
                            "    1 - DESPOTISM,\n" +
                            "    2 - NOOCRACY,\n" +
                            "    3 - TECHNOCRACY,\n" +
                            "    4 - TIMOCRACY.");
                }
            }

            int standardOfLivingCode = Integer.parseInt(args[9]);
            if (standardOfLivingCode < 1 || standardOfLivingCode > StandardOfLiving.values().length) {
                return new Response("Опять ты с уровнем жизни накосячил, коды от 1 до " + StandardOfLiving.values().length + ":\n" +
                        "    1 - ULTRA_HIGH,\n" +
                        "    2 - VERY_HIGH,\n" +
                        "    3 - LOW,\n" +
                        "    4 - VERY_LOW,\n" +
                        "    5 - ULTRA_LOW.");
            }
            StandardOfLiving standardOfLiving = StandardOfLiving.values()[standardOfLivingCode - 1];

            Human governor = null;
            if (args.length == 11 && !"null".equalsIgnoreCase(args[10])) {
                governor = new Human(args[10]);
            }

            City updatedCity = new City();
            updatedCity.setId(key); // Устанавливаем ID обновляемого элемента равным ключу
            updatedCity.setName(name);
            updatedCity.setCoordinates(new Coordinates(coordinateX, coordinateY));
            updatedCity.setArea(area);
            updatedCity.setPopulation(population);
            updatedCity.setMetersAboveSeaLevel(metersAboveSeaLevel);
            updatedCity.setClimate(climate);
            updatedCity.setGovernment(government);
            updatedCity.setStandardOfLiving(standardOfLiving);
            updatedCity.setGovernor(governor);

            updatedCity.validate(); // Валидация объекта City

            if (!collectionManager.containsKey(key)) {
                return new Response("Нет такого ключа " + key + ", что обновлять-то?");
            }

            if (collectionManager.update(key, updatedCity, authenticatedUsername)) {
                return new Response("Элемент с ключом " + key + " успешно обновлен.");
            } else {
                return new Response("Не удалось обновить элемент с ключом " + key + ", что-то пошло не так или вы не являетесь владельцем этого города.");
            }

        } catch (NumberFormatException e) {
            return new Response("Ты с числами совсем не дружишь: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return new Response("Аргументы какие-то кривые (возможно, неверные значения для ENUM): " + e.getMessage());
        } catch (ValidationException e) {
            return new Response("Город какой-то неправильный: " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            return new Response("Ты с кодами опять набедокурил (выход за пределы массива ENUM): " + e.getMessage());
        } catch (SQLException e) {
            return new Response("Ой, не получилось обновить в БД: " + e.getMessage());
        } catch (Exception e) {
            return new Response("Неожиданная ошибка при обновлении: " + e.getMessage());
        }
    }
}