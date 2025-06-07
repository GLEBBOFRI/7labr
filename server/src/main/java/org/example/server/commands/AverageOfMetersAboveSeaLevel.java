package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;

public class AverageOfMetersAboveSeaLevel extends Command {
    private final CollectionManager collectionManager;

    public AverageOfMetersAboveSeaLevel(CollectionManager collectionManager) {
        super("average_of_meters_above_sea_level", "вывести среднее значение высоты над уровнем моря для всех элементов коллекции");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) { // Изменена сигнатура
        // Для этой команды аутентификация не требуется, так как это просмотр.
        double average = collectionManager.getAverageMetersAboveSeaLevel();
        return new Response("среднее значение высоты над уровнем моря: " + average);
    }
}