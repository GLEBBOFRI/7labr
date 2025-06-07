package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;
import java.sql.SQLException;

public class RemoveLowerKey extends Command {
    private final CollectionManager collectionManager;

    public RemoveLowerKey(CollectionManager collectionManager) {
        super("remove_lower_key", "удалить из коллекции все элементы, ключ которых меньше, чем заданный (удаляет только ваши города)");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) { // Изменена сигнатура
        if (authenticatedUsername == null) {
            return new Response("Ошибка: Для выполнения команды 'remove_lower_key' требуется аутентификация. Пожалуйста, используйте 'register' или 'login'.");
        }
        try {
            Object[] args = (Object[]) request.getArguments();
            if (args == null || args.length == 0) {
                return new Response("а меньше какого ключа удалять-то будем?");
            }
            int key = Integer.parseInt(args[0].toString());
            int removedCount = collectionManager.removeLowerKey(key, authenticatedUsername);
            return new Response("удалено " + removedCount + " ваших элементов с ключом меньше чем " + key);
        } catch (NumberFormatException e) {
            return new Response("ты хоть ключ-то правильно введи, бестолочь: " + e.getMessage());
        } catch (SQLException e) {
            return new Response("что-то сломалось при удалении маленьких ключей: " + e.getMessage());
        } catch (Exception e) {
            return new Response("неожиданная ошибка при удалении маленьких ключей: " + e.getMessage());
        }
    }
}