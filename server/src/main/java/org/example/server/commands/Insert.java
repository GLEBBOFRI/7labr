package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import org.example.collection.models.*;
import org.example.collection.exceptions.ValidationException;

public class Insert extends Command {
    private final CollectionManager collectionManager;

    public Insert(CollectionManager collectionManager) {
        super("insert", "добавить новый элемент с автоматически сгенерированным ID");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        Object[] args = (Object[]) request.getArguments();
        if (args == null || args.length < 9 || args.length > 10) {
            return new Response("эй, полегче! аргументов не хватает или ты лишнего накидал: название, x, y, площадь, население, высота, код_климата, код_правительства, код_уровня_жизни [, имя_губернатора]");
        }

        try {
            String name = (String) args[0];
            Integer coordinateX = Integer.parseInt((String) args[1]);
            Long coordinateY = Long.parseLong((String) args[2]);
            Integer area = Integer.parseInt((String) args[3]);
            Long population = Long.parseLong((String) args[4]);
            Float metersAboveSeaLevel = Float.parseFloat((String) args[5]);

            int climateCode = Integer.parseInt((String) args[6]);
            if (climateCode < 1 || climateCode > Climate.values().length) {
                return new Response("ты с климатом ошибся, коды от 1 до " + Climate.values().length + ":\n" +
                        "    1 - RAIN_FOREST,\n" +
                        "    2 - HUMIDSUBTROPICAL,\n" +
                        "    3 - HUMIDCONTINENTAL,\n" +
                        "    4 - TUNDRA,\n" +
                        "    5 - POLAR_ICECAP");
            }
            Climate climate = Climate.values()[climateCode - 1];

            int governmentCode = Integer.parseInt((String) args[7]);
            if (governmentCode < 1 || governmentCode > Government.values().length) {
                return new Response("правительство тоже не то, коды от 1 до " + Government.values().length + ":\n" +
                        "    1 - DESPOTISM,\n" +
                        "    2 - NOOCRACY,\n" +
                        "    3 - TECHNOCRACY,\n" +
                        "    4 - TIMOCRACY");
            }
            Government government = Government.values()[governmentCode - 1];

            int standardOfLivingCode = Integer.parseInt((String) args[8]);
            if (standardOfLivingCode < 1 || standardOfLivingCode > StandardOfLiving.values().length) {
                return new Response("ну и уровень жизни у тебя... коды от 1 до " + StandardOfLiving.values().length + ":\n" +
                        "    1 - ULTRA_HIGH,\n" +
                        "    2 - VERY_HIGH,\n" +
                        "    3 - LOW,\n" +
                        "    4 - VERY_LOW,\n" +
                        "    5 - ULTRA_LOW");
            }
            StandardOfLiving standardOfLiving = StandardOfLiving.values()[standardOfLivingCode - 1];

            Human governor = null;
            if (args.length == 10) {
                governor = new Human((String) args[9]);
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

            newCity.validate();

            collectionManager.addElement(null, newCity); // Передаем null в качестве ключа

            return new Response("город успешно добавлен с id: " + newCity.getId());

        } catch (NumberFormatException e) {
            return new Response("ты что-то не то с числами намудрил");
        } catch (IllegalArgumentException e) {
            return new Response("аргументы какие-то кривые");
        } catch (ValidationException e) {
            return new Response("город у тебя какой-то неправильный: " + e.getMessage());
        } catch (IndexOutOfBoundsException e) {
            return new Response("ты с кодами совсем запутался");
        } catch (Exception e) {
            return new Response("ой, что-то пошло не так при добавлении города: " + e.getMessage());
        }
    }
}