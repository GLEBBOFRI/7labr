package org.example.comands;

import org.example.CommandManager;
import org.example.consol.Console;
import org.example.exceptions.CommandExecutionError;

public class Help extends Command {
    private final Console console;
    private final CommandManager commandManager;

    public Help(Console console, CommandManager commandManager) {
        super("help", "угадай что делает");
        this.console = console;
        this.commandManager = commandManager;
    }

    @Override
    public void execute(String[] args) throws CommandExecutionError {
        console.writeln("доступные комадны:");
        console.writeln("==================");

        commandManager.getAllCommands().forEach(cmd -> {
            console.writeln(String.format("%-25s - %s",
                    cmd.getName(), cmd.getDescription()));
        });

        console.writeln("\nдля подробностей, напиши: help <command>");
    }
}