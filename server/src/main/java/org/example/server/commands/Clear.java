package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;
import java.sql.SQLException;

public class Clear extends Command {
    private final CollectionManager collectionManager;

    public Clear(CollectionManager collectionManager) {
        super("clear", "очистить коллекцию (удаляет только ваши города)");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) { // Изменена сигнатура
        if (authenticatedUsername == null) {
            return new Response("Ошибка: Для выполнения команды 'clear' требуется аутентификация. Пожалуйста, используйте 'register' или 'login'.");
        }
        try {
            int removedCount = collectionManager.clearCollection(authenticatedUsername);
            if (removedCount > 0) {
                return new Response("коллекция успешно очищена. Удалено " + removedCount + " ваших городов.");
            } else {
                return new Response("в вашей коллекции нет городов для удаления.");
            }
        } catch (SQLException e) {
            return new Response("ой, ну ты и рукожоп, не смог коллекцию очистить: " + e.getMessage());
        }
    }
}