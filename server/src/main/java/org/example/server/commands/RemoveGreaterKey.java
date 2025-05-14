package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;

public class RemoveGreaterKey extends Command {
    private final CollectionManager collectionManager;

    public RemoveGreaterKey(CollectionManager collectionManager) {
        super("remove_greater_key", "удалить из коллекции все элементы, ключ которых превышает заданный");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String arg = (String) request.getArguments();
        if (arg == null || arg.isEmpty()) {
            return new Response("ключ-то где?");
        }
        try {
            int key = Integer.parseInt(arg);
            int removedCount = collectionManager.removeGreaterKey(key);
            return new Response("удалено " + removedCount + " элементов с ключом больше чем " + key);
        } catch (NumberFormatException e) {
            return new Response("ты ключ-то числом введи, балбес");
        } catch (Exception e) {
            return new Response("что-то пошло не так при удалении больших ключей: " + e.getMessage());
        }
    }
}