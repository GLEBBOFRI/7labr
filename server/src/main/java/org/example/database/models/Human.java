package org.example.database.models;

import java.io.Serializable;

public class Human implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;

    public Human(String name) {
        this.name = name;
    }

    public String getName() { return name; }

    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return "Human{" +
                "name='" + name + '\'' +
                '}';
    }

    public void validate() throws IllegalArgumentException {
        if (name == null || name.trim().isEmpty()) throw new IllegalArgumentException("Имя губернатора не может быть null или пустым.");
    }
}