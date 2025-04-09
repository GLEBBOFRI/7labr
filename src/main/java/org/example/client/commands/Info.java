package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.collection.utils.CollectionInfo;
import org.example.client.exceptions.CommandExecutionError;

public class Info extends Command {
    private final Console console;
    private final CollectionManager collectionManager;

    public Info(Console console, CollectionManager collectionManager) {
        super("info", "вывести информацию о коллекции");
        this.console = console;
        this.collectionManager = collectionManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 0) {
            throw new CommandExecutionError("смотри брат эта команда без аргументов");
        }

        try {
            // перезагружаем
            collectionManager.reloadCollectionFromFile();

            // достаем актуальную информацию
            CollectionInfo info = collectionManager.getCollectionInfo();

            // выводим
            console.writeln("Тип коллекции: " + info.getCollectionType());
            console.writeln("Количество элементов: " + info.getSize());
            console.writeln("Название файла инициализации: " + info.getLoadedFrom());
            return true;
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при загрузке данных из файла: " + e.getMessage());
        }
    }
}
