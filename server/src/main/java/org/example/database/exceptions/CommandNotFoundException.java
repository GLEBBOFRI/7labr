package org.example.database.exceptions;

public class CommandNotFoundException extends ControllerException {
    public CommandNotFoundException(String commandName) {
        super("команда " + commandName + " не найдена");
    }
}
