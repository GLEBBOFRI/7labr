package org.example.database.interfaces;

import org.example.database.exceptions.ValidationException;

public interface Validatable {
    void validate() throws ValidationException;
}
