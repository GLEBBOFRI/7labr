package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.collection.models.StandardOfLiving;
import org.example.client.exceptions.CommandExecutionError;

public class RemoveAllByStandardOfLiving extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public RemoveAllByStandardOfLiving(Console console, CollectionManager collectionManager) {
        super("remove_all_by_standard_of_living", "удалить из коллекции все элементы, значение поля standardOfLiving которых эквивалентно заданному");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 1) {
            throw new CommandExecutionError("смотри брат эта команда с 1 аргументом просто напиши remove_all_by_standard_of_living <StandardOfLiving>");
        }

        try {
            // передевываем строку аргумента в Enum
            StandardOfLiving standardOfLiving;
            try {
                standardOfLiving = StandardOfLiving.valueOf(arguments[0].toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new CommandExecutionError("недопустимое значение StandardOfLiving: " + arguments[0]);
            }

            // убивеам
            long removedCount = collectionManager.getCollection().values().stream()
                    .filter(city -> standardOfLiving.equals(city.getStandardOfLiving()))
                    .count();

            collectionManager.getCollection().values().removeIf(city -> standardOfLiving.equals(city.getStandardOfLiving()));
            console.writeln("удалено элементов: " + removedCount);
            return true;
        } catch (CommandExecutionError e) {
            throw e;
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при удалении элементов: " + e.getMessage());
        }
    }
}
