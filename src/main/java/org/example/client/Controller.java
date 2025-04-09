package org.example.client;

import org.example.client.console.Console;
import org.example.client.exceptions.CommandExecutionError;
import org.example.client.exceptions.CommandNotFoundException;

import java.util.Arrays;

public class Controller {
    private final Console console;
    private final CommandManager commandManager;

    public Controller(CommandManager commandManager, Console console) {
        this.console = console;
        this.commandManager = commandManager;
    }

    public void run() {
        String input;
        while ((input = console.read("напиши в меня полностью ")) != null) {
            try {
                console.writeln(handleInput(input));
            } catch (Exception e) {
                console.writeln(e.getMessage());
            }
        }
        console.writeln("программа завершена спасибо за внимание");
    }

    public String handleInput(String input) {
        try {
            parseInput(input);
            return "";
        } catch (CommandExecutionError e) {
            return e.getMessage();
        } catch (CommandNotFoundException e) {
            return e.getMessage() + " напиши help если не справился с тем чтобы зайти на se.ifmo.ru и ввести вариант 3432";
        }
    }

    private boolean parseInput(String input) throws CommandExecutionError {
        String[] data = input.split(" ");
        String commandName = data[0];
        Command command = commandManager.getCommand(commandName);
        return command.apply(Arrays.copyOfRange(data, 1, data.length));
    }
}