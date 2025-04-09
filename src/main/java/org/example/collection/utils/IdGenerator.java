package org.example.collection.utils;

import org.example.collection.exceptions.ValidationException;

public class IdGenerator {
    private long nextId = 1;

    // получение следующего доступного ID
    public long getNextId() {
        return nextId++;
    }

    // установка следующего доступного ID вручную
    public void setNextId(long nextId) {
        if (nextId > this.nextId) {
            this.nextId = nextId;
        }
    }
}
