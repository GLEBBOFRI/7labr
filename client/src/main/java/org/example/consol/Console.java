package org.example.consol;

public interface Console extends AutoCloseable {
    String read();
    default String read(String message) {
        write(message);
        return read();
    }
    void write(String data);
    default void writeln(String data) {
        write(data + "\n");
    }
}