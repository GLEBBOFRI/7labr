package org.example;

import org.example.client.*;
import org.example.client.commands.*;
import org.example.client.console.*;
import org.example.collection.*;
import org.example.collection.exceptions.*;
import org.example.collection.models.*;

import java.io.IOException;

public class Main {
    public static void main(String... args) {
        if (args.length == 0) {
            System.err.println("ошибка: не указан файл для загрузки коллекции.");
            System.err.println("напиши: java -jar 5labr-1.0-SNAPSHOT.jar <имя_файла.json>");
            System.exit(1);
        }

        StandartConsole console = new StandartConsole();
        CommandManager commandManager = new CommandManager();
        CollectionManager collectionManager = new CollectionManager();
        Controller controller = new Controller(commandManager, console);

        try {
            collectionManager.loadCollection(args[0]);
        } catch (IOException | ValidationException e) {
            System.err.println("ошибка при загрузке коллекции: " + e.getMessage());
            System.exit(1);
        }

        commandManager.registerCommand(new Help(console, commandManager));
        commandManager.registerCommand(new Info(console, collectionManager));
        commandManager.registerCommand(new Show(console, collectionManager));
        commandManager.registerCommand(new Insert(console, collectionManager));
        commandManager.registerCommand(new Update(console, collectionManager));
        commandManager.registerCommand(new RemoveKey(console, collectionManager));
        commandManager.registerCommand(new Clear(console, collectionManager));
        commandManager.registerCommand(new Save(console, collectionManager));
        commandManager.registerCommand(new ExecuteScript(console, commandManager));
        commandManager.registerCommand(new Exit());
        commandManager.registerCommand(new ReplaceIfGreater(console, collectionManager));
        commandManager.registerCommand(new RemoveGreaterKey(console, collectionManager));
        commandManager.registerCommand(new RemoveLowerKey(console, collectionManager));
        commandManager.registerCommand(new RemoveAllByStandardOfLiving(console, collectionManager));
        commandManager.registerCommand(new AverageOfMetersAboveSeaLevel(console, collectionManager));
        commandManager.registerCommand(new FilterStartsWithName(console, collectionManager));

        controller.run();
    }
}