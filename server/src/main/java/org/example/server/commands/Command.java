package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;

import java.util.Objects;

public abstract class Command {
    private final String name;
    private final String description;

    public Command(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Выполняет команду.
     * @param request Запрос от клиента.
     * @param authenticatedUsername Имя аутентифицированного пользователя, выполнившего запрос.
     * Может быть null, если пользователь не аутентифицирован.
     * @return Объект Response с результатом выполнения команды.
     */
    public abstract Response execute(Request request, String authenticatedUsername); // Изменена сигнатура

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Command command = (Command) o;
        return Objects.equals(name, command.name) &&
                Objects.equals(description, command.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, description);
    }

    @Override
    public String toString() {
        return "Command{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}