package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.client.exceptions.CommandExecutionError;

public class FilterStartsWithName extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public FilterStartsWithName(Console console, CollectionManager collectionManager) {
        super("filter_starts_with_name", "вывести элементы, значение поля name которых начинается с заданной подстроки");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 1) throw new CommandExecutionError("смотри брат эта команда с 1 аргументом просто напиши filter_starts_with_name <name>");
        String prefix = arguments[0];
        collectionManager.getCollection().values().stream()
                .filter(city -> city.getName().startsWith(prefix))
                .forEach(city -> console.writeln(city.toString()));
        return true;
    }
}