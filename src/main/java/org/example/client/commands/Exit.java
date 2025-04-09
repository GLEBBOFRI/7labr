package org.example.client.commands;

import org.example.client.Command;
import org.example.client.console.Console;
import org.example.client.exceptions.CommandExecutionError;

public class Exit extends Command {
    public Exit() {
        super("exit", "завершить программу");
    }
    Console console;
    public static void stop(String[] args) {
        try {
            byte[] b = new byte[1024];
            for (int r; (r = System.in.read(b)) != -1;) {
                String buffer = new String(b, 0, r);
                System.out.println("read: " + buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean apply(String[] arguments) throws CommandExecutionError {
        System.out.println("программа завершена по команде exit, дай бог свидимся");
        System.exit(0);
        return true;
    }
}
