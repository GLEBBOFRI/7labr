package org.example.database.exceptions;

public class FieldLowerThanValidException extends ValidationException {

    public FieldLowerThanValidException(String field, Number Bound) {
        super("значение " + field + " не должно быть меньше чем " + Bound.toString() + ".");
    }
}
