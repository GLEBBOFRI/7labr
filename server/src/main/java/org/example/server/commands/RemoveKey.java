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
        try {
            Object[] args = (Object[]) request.getArguments();
            if (args == null || args.length == 0) {
                return new Response("ну и какой ключ удалять будем, умник?");
            }
            int key = Integer.parseInt(args[0].toString());
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