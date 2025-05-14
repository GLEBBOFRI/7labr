package org.example.interfaces;

import org.example.exceptions.ValidationException;

public interface Validatable {
    void validate() throws ValidationException;
}
