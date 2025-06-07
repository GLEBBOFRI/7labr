package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;
import java.sql.SQLException;

public class RemoveKey extends Command {
    private final CollectionManager collectionManager;

    public RemoveKey(CollectionManager collectionManager) {
        super("remove_key", "удалить элемент из коллекции по его ключу (удаляет только ваши города)");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) { // Изменена сигнатура
        if (authenticatedUsername == null) {
            return new Response("Ошибка: Для выполнения команды 'remove_key' требуется аутентификация. Пожалуйста, используйте 'register' или 'login'.");
        }
        try {
            Object[] args = (Object[]) request.getArguments();
            if (args == null || args.length == 0) {
                return new Response("ну и какой ключ удалять будем, умник?");
            }
            int key = Integer.parseInt(args[0].toString());
            if (collectionManager.remove(key, authenticatedUsername)) { // Передаем authenticatedUsername
                return new Response("элемент с ключом " + key + " тю-тю, удален");
            } else {
                return new Response("нет такого ключа " + key + " или вы не являетесь его владельцем.");
            }
        } catch (NumberFormatException e) {
            return new Response("ключ - это циферки, алло: " + e.getMessage());
        } catch (SQLException e) {
            return new Response("удалить-то не получилось из БД: " + e.getMessage());
        } catch (Exception e) {
            return new Response("неожиданная ошибка при удалении: " + e.getMessage());
        }
    }
}