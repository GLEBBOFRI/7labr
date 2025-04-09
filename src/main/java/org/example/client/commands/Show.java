package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.client.exceptions.CommandExecutionError;

public class Show extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public Show(Console console, CollectionManager collectionManager) {
        super("show", "вывести все элементы коллекции");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 0) {
            throw new CommandExecutionError("смотри брат эта команда без аргументов");
        }

        try {
            // загружаем коллекцию
            collectionManager.reloadCollectionFromFile();

            // выводим
            String collectionString = collectionManager.toString();
            if (collectionString.isEmpty()) {
                console.writeln("коллекция пустая");
            } else {
                console.writeln(collectionString);
            }
            return true;
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при выводе коллекции: " + e.getMessage());
        }
    }
}
