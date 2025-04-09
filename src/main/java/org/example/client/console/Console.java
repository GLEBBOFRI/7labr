package org.example.client.console;

import java.io.IOException;

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