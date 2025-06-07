package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;
import java.sql.SQLException;

public class RemoveGreaterKey extends Command {
    private final CollectionManager collectionManager;

    public RemoveGreaterKey(CollectionManager collectionManager) {
        super("remove_greater_key", "удалить из коллекции все элементы, ключ которых превышает заданный (удаляет только ваши города)");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) { // Изменена сигнатура
        if (authenticatedUsername == null) {
            return new Response("Ошибка: Для выполнения команды 'remove_greater_key' требуется аутентификация. Пожалуйста, используйте 'register' или 'login'.");
        }
        try {
            Object[] args = (Object[]) request.getArguments();
            if (args == null || args.length == 0) {
                return new Response("ключ-то где?");
            }
            int key = Integer.parseInt(args[0].toString());
            int removedCount = collectionManager.removeGreaterKey(key, authenticatedUsername);
            return new Response("удалено " + removedCount + " ваших элементов с ключом больше чем " + key);
        } catch (NumberFormatException e) {
            return new Response("ты ключ-то числом введи, балбес: " + e.getMessage());
        } catch (SQLException e) {
            return new Response("что-то пошло не так при удалении больших ключей: " + e.getMessage());
        } catch (Exception e) {
            return new Response("неожиданная ошибка при удалении больших ключей: " + e.getMessage());
        }
    }
}