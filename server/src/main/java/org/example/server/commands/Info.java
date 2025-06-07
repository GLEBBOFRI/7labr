package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;

public class Info extends Command {
    private final CollectionManager collectionManager;

    public Info(CollectionManager collectionManager) {
        super("info", "вывести в стандартный поток вывода информацию о коллекции");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request, String authenticatedUsername) { // Изменена сигнатура
        // Для этой команды аутентификация не требуется, так как это просмотр.
        return new Response(collectionManager.getCollectionInfo());
    }
}