package org.example.client.commands;

import org.example.client.Command;
import org.example.client.CommandManager;
import org.example.client.console.Console;
import org.example.client.exceptions.CommandExecutionError;

import java.util.List;

public class Help extends Command {
    private final Console console;
    private final CommandManager commandManager;

    public Help(Console console, CommandManager commandManager) {
        super("help", "вывести справку по доступным командам");
        this.console = console;
        this.commandManager = commandManager;
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        if (arguments.length != 0) throw new CommandExecutionError("смотри брат эта команда без аргументов");
        List<Command> commands = commandManager.getAllCommands();
        console.writeln("доступные команды:");
        for (Command command : commands) {
            console.writeln(command.getName() + " - " + command.getDescription());
        }
        return true;
    }
}