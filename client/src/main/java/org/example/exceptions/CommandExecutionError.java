package org.example.exceptions;

public class CommandExecutionError extends Exception {
    public CommandExecutionError() {
        super("знаешь ошибки? я ошибся (с) Дж Стетхем, ладно произошла ошибка при выполнении команды иди офай комп тебе ту не рады");
    }

    public CommandExecutionError(String message) {
        super(message);
    }
}
