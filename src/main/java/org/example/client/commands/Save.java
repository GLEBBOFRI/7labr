package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.client.exceptions.CommandExecutionError;

import java.io.IOException;

public class Save extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public Save(Console console, CollectionManager collectionManager) {
        super("save", "сохранить коллекцию в файл");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 0) throw new CommandExecutionError("смотри брат эта команда без аргументов");
        try {
            collectionManager.saveCollection("output.json");
            console.writeln("коллекция успешно сохранена");
            return true;
        } catch (IOException e) {
            throw new CommandExecutionError("ошибка при сохранении коллекции: " + e.getMessage());
        }
    }
}