package org.example.collection.models;

import org.example.collection.exceptions.ValidationException;

public class Human {
    private String name; // Поле не может быть null, Строка не может быть пустой

    public Human(String name) {
        this.name = name;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public void validate() throws ValidationException {
        if (name == null || name.isEmpty()) throw new ValidationException("неверное имя");
    }

    @Override
    public String toString() {
        return "Human{" +
                "name='" + name + '\'' +
                '}';
    }
}