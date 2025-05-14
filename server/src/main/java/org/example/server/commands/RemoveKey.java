package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;

public class RemoveKey extends Command {
    private final CollectionManager collectionManager;

    public RemoveKey(CollectionManager collectionManager) {
        super("remove_key", "удалить элемент из коллекции по его ключу");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String arg = (String) request.getArguments();
        if (arg == null || arg.isEmpty()) {
            return new Response("ну и какой ключ удалять будем, умник?");
        }
        try {
            int key = Integer.parseInt(arg);
            if (collectionManager.remove(key)) {
                return new Response("элемент с ключом " + key + " тю-тю, удален");
            } else {
                return new Response("нет такого ключа " + key + ", ты что-то путаешь");
            }
        } catch (NumberFormatException e) {
            return new Response("ключ - это циферки, алло");
        } catch (Exception e) {
            return new Response("удалить-то не получилось: " + e.getMessage());
        }
    }
}