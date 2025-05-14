package org.example.collection.exceptions;

import org.example.collection.exceptions.ControllerException;

public class CommandNotFoundException extends ControllerException {
    public CommandNotFoundException(String commandName) {
        super("команда " + commandName + " не найдена");
    }
}
