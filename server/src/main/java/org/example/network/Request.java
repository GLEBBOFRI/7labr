package org.example.network;

import java.io.Serializable;
import java.util.Arrays;

public class Request implements Serializable {

    private static final long serialVersionUID = 58295393L;

    private final String commandName;
    private final Object arguments;

    public Request(String commandName, Object arguments) {
        this.commandName = commandName;
        this.arguments = arguments;
    }

    public String getCommandName() {
        return commandName;
    }

    public Object getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        String argsString;
        if (arguments == null) {
            argsString = "null";
        } else if (arguments instanceof Object[]) {
            argsString = Arrays.toString((Object[]) arguments);
        } else if (arguments instanceof String[]) {
            argsString = Arrays.toString((String[]) arguments);
        } else {
            argsString = arguments.toString();
        }
        return "Request{" +
                "commandName='" + commandName + '\'' +
                ", arguments=" + argsString +
                '}';
    }
}
