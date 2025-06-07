package org.example.database.exceptions;

public class EmptyFieldException extends ValidationException {
    public EmptyFieldException(String field) {
        super("поле " + field + "не может быть пустым");
    }
}
