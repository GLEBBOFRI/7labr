package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.client.exceptions.CommandExecutionError;
import org.example.collection.models.City;

public class AverageOfMetersAboveSeaLevel extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public AverageOfMetersAboveSeaLevel(Console console, CollectionManager collectionManager) {
        super("average_of_meters_above_sea_level", "вывести среднее значение высоты над уровнем моря для всех элементов коллекции");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 0) throw new CommandExecutionError("смотри брат эта команда без аргументов");
        double average = collectionManager.getCollection().values().stream()
                .mapToDouble(City::getMetersAboveSeaLevel)
                .average()
                .orElse(0);
        console.writeln("среднее значение высоты над уровнем моря: " + average);
        return true;
    }
}