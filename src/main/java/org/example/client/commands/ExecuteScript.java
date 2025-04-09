package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.collection.CollectionManager;
import org.example.client.exceptions.CommandExecutionError;
import org.example.client.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class ExecuteScript extends Command {
    private final Console console;
    private final CommandManager commandManager;

    public ExecuteScript(Console console, CommandManager commandManager) {
        super("execute_script", "считать и исполнить скрипт из указанного файла");
        this.console = console;
        this.commandManager = commandManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 1) {
            throw new CommandExecutionError("смотри брат эта команда с 1 аргументом просто напиши execute_script <имя_файла>");
        }

        try {
            File file = new File(arguments[0]);
            Scanner scanner = new Scanner(file);

            while (scanner.hasNextLine()) {
                String commandLine = scanner.nextLine().trim();
                if (commandLine.isEmpty()) continue;

                // разделение на команду и аргументы
                String[] parts = commandLine.split(" ");
                String commandName = parts[0];
                String[] commandArgs = Arrays.copyOfRange(parts, 1, parts.length);

                Command command = commandManager.getCommand(commandName);
                if (command != null) {
                    command.apply(commandArgs);
                } else {
                    console.writeln("команда \"" + commandName + "\" не найдена");
                }
            }
            scanner.close();
            return true;
        } catch (FileNotFoundException e) {
            throw new CommandExecutionError("файл не найден: " + arguments[0]);
        } catch (Exception e) {
            throw new CommandExecutionError("ошибка при выполнении скрипта: " + e.getMessage());
        }
    }
}
