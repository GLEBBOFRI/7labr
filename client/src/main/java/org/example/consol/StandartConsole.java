package org.example.consol;

import java.util.Scanner;

public class StandartConsole implements Console {
    private final Scanner scanner = new Scanner(System.in);

    public StandartConsole() {
    }

    @Override
    public String read() {
        return scanner.nextLine();
    }

    @Override
    public String read(String message) {
        write(message);
        return read();
    }

    @Override
    public void write(String data) {
        System.out.print(data);
    }

    @Override
    public void writeln(String data) {
        System.out.println(data);
    }

    @Override
    public void close() {
        scanner.close();
    }
}