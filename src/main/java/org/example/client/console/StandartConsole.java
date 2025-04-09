package org.example.client.console;

import java.io.*;

public class StandartConsole implements Console {
    private final BufferedReader consoleReader = new BufferedReader(new InputStreamReader(System.in));
    private final BufferedWriter consoleWriter = new BufferedWriter(new OutputStreamWriter(System.out));

    @Override
    public String read() {
        try {
            return consoleReader.readLine();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void write(String data) {
        try {
            consoleWriter.append(data).flush();
        } catch (IOException e) {
            throw new RuntimeException("ошибка вывода в консоль ты че там написанинил?");
        }
    }

    @Override
    public void close() throws Exception {
        consoleReader.close();
        consoleWriter.close();
    }
}