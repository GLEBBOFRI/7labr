package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;
import org.example.database.models.*;
import org.example.database.exceptions.ValidationException;
import java.sql.SQLException;

public class Insert extends Command {
    private final CollectionManager collectionManager;

    public Insert(CollectionManager collectionManager) {
        super("insert", "добавить новый элемент с автоматически сгенерированным ID. Использование: insert <название> <x> <y> <площадь> <население> <высота> <код_климата> <код_правительства> <код_уровня_жизни> [<имя_губернатора>]");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) {
        if (authenticatedUsername == null) {
            return new Response("Ошибка: Для выполнения команды 'insert' требуется аутентификация. Пожалуйста, используйте 'register' или 'login'.");
        }

        String[] args = (String[]) request.getArguments(); // Теперь аргументы - это String[]
        if (args == null || args.length < 9 || args.length > 10) {
            return new Response("Эй, полегче! Аргументов не хватает или ты лишнего накидал: название, x, y, площадь, население, высота, код_климата, код_правительства, код_уровня_жизни [, имя_губернатора].");
        }

        try {
            String name = args[0];
            Integer coordinateX = Integer.parseInt(args[1]);
            Long coordinateY = Long.parseLong(args[2]);
            Integer area = Integer.parseInt(args[3]);
            Long population = Long.parseLong(args[4]);
            Float metersAboveSeaLevel = "null".equalsIgnoreCase(args[5]) ? null : Float.parseFloat(args[5]);

            int climateCode = Integer.parseInt(args[6]);
            if (climateCode < 1 || climateCode > Climate.values().length) {
                return new Response("Ты с климатом ошибся, коды от 1 до " + Climate.values().length + ":\n" +
                        "    1 - RAIN_FOREST,\n" +
                        "    2 - HUMIDSUBTROPICAL,\n" +
                        "    3 - HUMIDCONTINENTAL,\n" +
                        "    4 - TUNDRA,\n" +
                        "    5 - POLAR_ICECAP.");
            }
            Climate climate = Climate.values()[climateCode - 1];

            Government government = null;
            if (!"null".equalsIgnoreCase(args[7])) {
                int governmentCode = Integer.parseInt(args[7]);
                if (governmentCode >= 1 && governmentCode <= Government.values().length) {
                    government = Government.values()[governmentCode - 1];
                } else {
                    return new Response("Правительство тоже не то, коды от 1 до " + Government.values().length + ":\n" +
                            "    1 - DESPOTISM,\n" +
                            "    2 - NOOCRACY,\n" +
                            "    3 - TECHNOCRACY,\n" +
                            "    4 - TIMOCRACY.");
                }
            }


            int standardOfLivingCode = Integer.parseInt(args[8]);
            if (standardOfLivingCode < 1 || standardOfLivingCode > StandardOfLiving.values().length) {
                return new Response("Ну и уровень жизни у тебя... коды от 1 до " + StandardOfLiving.values().length + ":\n" +
                        "    1 - ULTRA_HIGH,\n" +
                        "    2 - VERY_HIGH,\n" +
                        "    3 - LOW,\n" +
                        "    4 - VERY_LOW,\n" +
                        "    5 - ULTRA_LOW.");
            }
            StandardOfLiving standardOfLiving = StandardOfLiving.values()[standardOfLivingCode - 1];

            Human governor = null;
            if (args.length == 10 && !"null".equalsIgnoreCase(args[9])) {
                governor = new Human(args[9]);
            }

            City newCity = new City();
            newCity.setName(name);
            newCity.setCoordinates(new Coordinates(coordinateX, coordinateY));
            newCity.setArea(area);
            newCity.setPopulation(population);
            newCity.setMetersAboveSeaLevel(metersAboveSeaLevel);
            newCity.setClimate(climate);
            newCity.setGovernment(government);
            newCity.setStandardOfLiving(standardOfLiving);
            newCity.setGovernor(governor);

            newCity.validate(); // Валидация объекта City

            collectionManager.addElement(newCity, authenticatedUsername);

            return new Response("Город успешно добавлен.");

        } catch (NumberFormatException e) {
            return new Response("Ты что-то не то с числами намудрил: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            return new Response("Аргументы какие-то кривые (возможно, неверные значения для ENUM): " + e.getMessage());
        } catch (ValidationException e) {
            return new Response("Город у тебя какой-то неправильный: " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            return new Response("Ты с кодами совсем запутался (выход за пределы массива ENUM): " + e.getMessage());
        } catch (SQLException e) {
            return new Response("Ошибка базы данных при добавлении города: " + e.getMessage());
        } catch (Exception e) {
            return new Response("Ой, что-то пошло не так при добавлении города: " + e.getMessage());
        }
    }
}