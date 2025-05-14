package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;

public class RemoveLowerKey extends Command {
    private final CollectionManager collectionManager;

    public RemoveLowerKey(CollectionManager collectionManager) {
        super("remove_lower_key", "удалить из коллекции все элементы, ключ которых меньше, чем заданный");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String arg = (String) request.getArguments();
        if (arg == null || arg.isEmpty()) {
            return new Response("а меньше какого ключа удалять-то будем?");
        }
        try {
            int key = Integer.parseInt(arg);
            int removedCount = collectionManager.removeLowerKey(key);
            return new Response("удалено " + removedCount + " элементов с ключом меньше чем " + key);
        } catch (NumberFormatException e) {
            return new Response("ты хоть ключ-то правильно введи, бестолочь");
        } catch (Exception e) {
            return new Response("что-то сломалось при удалении маленьких ключей: " + e.getMessage());
        }
    }
}