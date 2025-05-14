package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import org.example.collection.models.*;
import org.example.collection.exceptions.ValidationException;

public class Update extends Command {
    private final CollectionManager collectionManager;

    public Update(CollectionManager collectionManager) {
        super("update", "обновить элемент по заданному ключу, получая все новые данные в одной строке");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        Object[] args = (Object[]) request.getArguments();
        if (args == null || args.length < 10 || args.length > 11) {
            return new Response("ты что-то напутал с аргументами, нужно: ключ, название, x, y, площадь, население, высота, код_климата, код_правительства, код_уровня_жизни [, имя_губернатора]");
        }

        try {
            Integer key;
            try {
                key = Integer.parseInt((String) args[0]);
            } catch (NumberFormatException e) {
                return new Response("ключ-то циферками введи, неуч");
            }

            String name = (String) args[1];
            Integer coordinateX = Integer.parseInt((String) args[2]);
            Long coordinateY = Long.parseLong((String) args[3]);
            Integer area = Integer.parseInt((String) args[4]);
            Long population = Long.parseLong((String) args[5]);
            Float metersAboveSeaLevel = Float.parseFloat((String) args[6]);

            int climateCode = Integer.parseInt((String) args[7]);
            if (climateCode < 1 || climateCode > Climate.values().length) {
                return new Response("с климатом опять беда, коды от 1 до " + Climate.values().length + ":\n" +
                        "    1 - RAIN_FOREST,\n" +
                        "    2 - HUMIDSUBTROPICAL,\n" +
                        "    3 - HUMIDCONTINENTAL,\n" +
                        "    4 - TUNDRA,\n" +
                        "    5 - POLAR_ICECAP");
            }
            Climate climate = Climate.values()[climateCode - 1];

            int governmentCode = Integer.parseInt((String) args[8]);
            if (governmentCode < 1 || governmentCode > Government.values().length) {
                return new Response("и с правительством тоже, коды от 1 до " + Government.values().length + ":\n" +
                        "    1 - DESPOTISM,\n" +
                        "    2 - NOOCRACY,\n" +
                        "    3 - TECHNOCRACY,\n" +
                        "    4 - TIMOCRACY");
            }
            Government government = Government.values()[governmentCode - 1];

            int standardOfLivingCode = Integer.parseInt((String) args[9]);
            if (standardOfLivingCode < 1 || standardOfLivingCode > StandardOfLiving.values().length) {
                return new Response("опять ты с уровнем жизни накосячил, коды от 1 до " + StandardOfLiving.values().length + ":\n" +
                        "    1 - ULTRA_HIGH,\n" +
                        "    2 - VERY_HIGH,\n" +
                        "    3 - LOW,\n" +
                        "    4 - VERY_LOW,\n" +
                        "    5 - ULTRA_LOW");
            }
            StandardOfLiving standardOfLiving = StandardOfLiving.values()[standardOfLivingCode - 1];

            Human governor = null;
            if (args.length == 11) {
                governor = new Human((String) args[10]);
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

            updatedCity.validate();

            if (!collectionManager.containsKey(key)) {
                return new Response("нет такого ключа " + key + ", что обновлять-то?");
            }

            if (collectionManager.update(key, updatedCity)) {
                return new Response("элемент с ключом " + key + " успешно обновлен");
            } else {
                return new Response("не удалось обновить элемент с ключом " + key + ", что-то пошло не так");
            }

        } catch (NumberFormatException e) {
            return new Response("ты с числами совсем не дружишь");
        } catch (IllegalArgumentException e) {
            return new Response("аргументы какие-то кривые");
        } catch (ValidationException e) {
            return new Response("город какой-то неправильный: " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            return new Response("ты с кодами опять набедокурил");
        } catch (Exception e) {
            return new Response("ой, не получилось обновить: " + e.getMessage());
        }
    }
}