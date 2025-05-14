package org.example.utils;

public class IdGenerator {
    private int nextId = 1;

    public synchronized int getNextId() {
        if (nextId == Integer.MAX_VALUE) {
            throw new IllegalStateException("Достигнут максимальный ID");
        }
        return nextId++;
    }

    public synchronized void setNextId(int nextId) {
        if (nextId <= 0) {
            throw new IllegalArgumentException("ID должен быть положительным");
        }
        this.nextId = Math.max(this.nextId, nextId);
    }
}