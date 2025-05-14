package org.example.collection.models;

import org.example.collection.exceptions.ValidationException;

public class Coordinates {
    private Integer x; // Значение поля должно быть больше -81, Поле не может быть null
    private Long y; // Поле не может быть null

    public Coordinates(Integer x, Long y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() { return x; }
    public void setX(Integer x) { this.x = x; }
    public Long getY() { return y; }
    public void setY(Long y) { this.y = y; }

    public void validate() throws ValidationException {
        if (x == null || x <= -81) throw new ValidationException("неверное значение x");
        if (y == null) throw new ValidationException("неверное значение y");
    }

    @Override
    public String toString() {
        return "Coordinates{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }
}