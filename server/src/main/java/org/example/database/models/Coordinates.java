package org.example.database.models;

import java.io.Serializable;

public class Coordinates implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer x;
    private Long y;

    public Coordinates(Integer x, Long y) {
        this.x = x;
        this.y = y;
    }

    public Integer getX() { return x; }
    public Long getY() { return y; }

    public void setX(Integer x) { this.x = x; }
    public void setY(Long y) { this.y = y; }

    @Override
    public String toString() {
        return "Coordinates{" +
                "x=" + x +
                ", y=" + y +
                '}';
    }

    public void validate() throws IllegalArgumentException {
        if (x == null || x <= -81) throw new IllegalArgumentException("Координата X должна быть больше -81.");
        if (y == null) throw new IllegalArgumentException("Координата Y не может быть null.");
    }
}