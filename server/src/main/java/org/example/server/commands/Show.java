package org.example.server.commands;

import org.example.collection.models.City;
import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import java.util.Collection;
import java.util.stream.Collectors;

public class Show extends Command {
    private final CollectionManager collectionManager;

    public Show(CollectionManager collectionManager) {
        super("show", "вывести в стандартный поток вывода все элементы коллекции");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        Collection<City> cities = collectionManager.getSortedCollection();
        if (cities.isEmpty()) {
            return new Response("да тут пусто, показывать-то нечего");
        } else {
            String data = cities.stream().map(City::toString).collect(Collectors.joining("\n"));
            return new Response("элементы коллекции:\n" + data);
        }
    }
}