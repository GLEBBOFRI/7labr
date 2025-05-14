package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import org.example.collection.models.City;
import org.example.collection.exceptions.ValidationException;

public class ReplaceIfGreater extends Command {
    private final CollectionManager collectionManager;

    public ReplaceIfGreater(CollectionManager collectionManager) {
        super("replace_if_greater", "заменить, если больше");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        try {
            Object[] args = (Object[]) request.getArguments();
            if (args == null || args.length < 2) {
                return new Response("ты что-то недоговариваешь, нужны ключ и город");
            }

            Integer key;
            try {
                key = Integer.parseInt((String) args[0]);
            } catch (NumberFormatException e) {
                return new Response("ключ-то числом будь добр введи");
            }

            City newCity;
            try {
                newCity = (City) args[1];
            } catch (ClassCastException e) {
                return new Response("а город-то где? ты что-то не то передал");
            }

            newCity.validate();
            if (!collectionManager.containsKey(key)) {
                return new Response("нет такого ключа " + key + ", куда ты собрался заменять?");
            }
            if (collectionManager.replaceIfGreater(key, newCity)) {
                return new Response("элемент с ключом " + key + " заменен, этот город круче");
            } else {
                return new Response("не стали менять, твой новый город так себе");
            }

        } catch (ValidationException e) {
            return new Response("город у тебя какой-то бракованный: " + e.getMessage());
        } catch (ClassCastException e) {
            return new Response("ты что-то перепутал с типами аргументов");
        } catch (Exception e) {
            return new Response("ой, не получилось заменить, что-то сломалось: " + e.getMessage());
        }
    }
}