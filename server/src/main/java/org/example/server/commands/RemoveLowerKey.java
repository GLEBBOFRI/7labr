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
        try {
            Object[] args = (Object[]) request.getArguments();
            if (args == null || args.length == 0) {
                return new Response("а меньше какого ключа удалять-то будем?");
            }
            int key = Integer.parseInt(args[0].toString());
            int removedCount = collectionManager.removeLowerKey(key);
            return new Response("удалено " + removedCount + " элементов с ключом меньше чем " + key);
        } catch (NumberFormatException e) {
            return new Response("ты хоть ключ-то правильно введи, бестолочь");
        } catch (Exception e) {
            return new Response("что-то сломалось при удалении маленьких ключей: " + e.getMessage());
        }
    }
}