package org.example.comands;

import org.example.comands.Command;
import org.example.CommandManager;
import org.example.consol.Console;
import org.example.exceptions.CommandExecutionError;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class ExecuteScript extends Command {
    private final Console console;
    private final CommandManager commandManager;

    public ExecuteScript(Console console, CommandManager commandManager) {
        super("execute_script", "считать и выполнить скрипт из указанного файла");
        this.console = console;
        this.commandManager = commandManager;
    }

    @Override
    public void execute(String[] args) throws CommandExecutionError {
        if (args.length != 1) {
            throw new CommandExecutionError("использование: execute_script <имя_файла>");
        }

        File scriptFile = new File(args[0]);
        if (!scriptFile.exists() || !scriptFile.isFile()) {
            throw new CommandExecutionError("файл не найден или не является файлом: " + args[0]);
        }

        try (Scanner scanner = new Scanner(scriptFile)) {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] parts = line.split("\\s+", 2);
                    String commandName = parts[0];
                    String[] commandArgs = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

                    if (commandName.equalsIgnoreCase("execute_script")) {
                        console.writeln("обнаружена рекурсивная попытка вызова execute_script иди отсюда, тебя тут не любят!");
                        continue;
                    }

                    Command command = commandManager.getCommand(commandName);
                    if (command != null) {
                        try {
                            command.execute(commandArgs);
                        } catch (CommandExecutionError e) {
                            console.writeln("ошибка при выполнении команды '" + commandName + "' из скрипта: " + e.getMessage());
                        }
                    } else {
                        console.writeln("команда '" + commandName + "', найденная в скрипте, написана рукожопом. Переделать!");
                    }
                }
            }
            console.writeln("выполнение скрипта '" + args[0] + "' завершено твои миллион городов загрузились в коллекцию!");
        } catch (Exception e) {
            throw new CommandExecutionError("произошла ошибка при чтении или выполнении скрипта: " + e.getMessage());
        }
    }
}