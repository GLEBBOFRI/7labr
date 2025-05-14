package org.example.collection.exceptions;


public class NullFieldException extends ValidationException {
    public NullFieldException(String field) {
        super("Значение поля " + field + " не должно быть null");
    }
}
