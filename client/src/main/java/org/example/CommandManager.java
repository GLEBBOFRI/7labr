package org.example;

import org.example.exceptions.CommandNotFoundException;
import java.util.ArrayList;
import java.util.List;
import org.example.comands.Command;

public class CommandManager {
    private final List<Command> commands = new ArrayList<>();

    public void registerCommand(Command command) {
        commands.add(command);
    }

    public Command getCommand(String commandName) throws CommandNotFoundException {
        return commands.stream()
                .filter(command -> commandName.equalsIgnoreCase(command.getName()))
                .findFirst()
                .orElseThrow(() -> new CommandNotFoundException(commandName));
    }

    public List<Command> getAllCommands() {
        return new ArrayList<>(commands);
    }
}