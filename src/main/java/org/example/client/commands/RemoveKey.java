package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.client.exceptions.CommandExecutionError;

public class RemoveKey extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public RemoveKey(Console console, CollectionManager collectionManager) {
        super("remove_key", "удалить элемент из коллекции по его ключу");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 1) {
            throw new CommandExecutionError("смотри брат эта команда с 1 аргументом просто напиши remove_key <ключ>");
        }

        try {
            // пампим нефть
            Integer key;
            try {
                key = Integer.parseInt(arguments[0]);
            } catch (NumberFormatException e) {
                throw new CommandExecutionError("ключ должен быть целым числом. введённое значение: " + arguments[0]);
            }

            // убиваем
            if (collectionManager.remove(key)) {
                console.writeln("элемент с ключом " + key + " успешно удалён.");
            } else {
                console.writeln("элемент с ключом " + key + " не найден в коллекции.");
            }

            return true;
        } catch (CommandExecutionError e) {
            throw e;
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при удалении элемента: " + e.getMessage());
        }
    }
}
