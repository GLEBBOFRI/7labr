package org.example.collection.interfaces;

import org.example.collection.exceptions.ValidationException;

public interface Validatable {
    void validate() throws ValidationException;
}
