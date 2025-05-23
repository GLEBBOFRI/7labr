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
        try {
            Object[] args = (Object[]) request.getArguments();
            if (args == null || args.length == 0) {
                return new Response("ключ-то где?");
            }
            int key = Integer.parseInt(args[0].toString());
            int removedCount = collectionManager.removeGreaterKey(key);
            return new Response("удалено " + removedCount + " элементов с ключом больше чем " + key);
        } catch (NumberFormatException e) {
            return new Response("ты ключ-то числом введи, балбес");
        } catch (Exception e) {
            return new Response("что-то пошло не так при удалении больших ключей: " + e.getMessage());
        }
    }
}