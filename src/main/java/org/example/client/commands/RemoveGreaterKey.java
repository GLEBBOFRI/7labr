package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.client.exceptions.CommandExecutionError;

public class RemoveGreaterKey extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public RemoveGreaterKey(Console console, CollectionManager collectionManager) {
        super("remove_greater_key", "удалить из коллекции все элементы, ключ которых превышает заданный");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 1) {
            throw new CommandExecutionError("смотри брат эта команда с 1 аргументом просто напиши remove_greater_key <ключ>");
        }

        try {
            Integer key;
            try {
                key = Integer.parseInt(arguments[0]);
            } catch (NumberFormatException e) {
                throw new CommandExecutionError("ключ должен быть целым числом. введённое значение: " + arguments[0]);
            }

            //убиваем
            long initialSize = collectionManager.getCollection().size();
            collectionManager.getCollection().keySet().removeIf(k -> k > key);
            long finalSize = collectionManager.getCollection().size();

            if (initialSize == finalSize) {
                console.writeln("нет элементов с ключами, превышающими " + key);
            } else {
                console.writeln("удалено элементов: " + (initialSize - finalSize));
            }

            return true;
        } catch (CommandExecutionError e) {
            throw e;
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при удалении элементов: " + e.getMessage());
        }
    }
}
