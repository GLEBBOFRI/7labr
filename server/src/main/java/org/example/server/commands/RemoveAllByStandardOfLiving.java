package org.example.server.commands;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;
import org.example.collection.models.StandardOfLiving;
import java.util.Arrays;
import java.util.stream.Collectors;

public class RemoveAllByStandardOfLiving extends Command {
    private final CollectionManager collectionManager;

    public RemoveAllByStandardOfLiving(CollectionManager collectionManager) {
        super("remove_all_by_standard_of_living", "удалить из коллекции все элементы, значение поля standardOfLiving которых эквивалентно заданному");
        this.collectionManager = collectionManager;
    }

    @Override
    public Response execute(Request request) {
        String arg = (String) request.getArguments();
        if (arg == null || arg.isEmpty()) {
            return new Response("а уровень жизни-то где?");
        }
        try {
            StandardOfLiving standard = StandardOfLiving.valueOf(arg.toUpperCase());
            int removedCount = collectionManager.removeAllByStandardOfLiving(standard);
            return new Response("удалено " + removedCount + " элементов с уровнем жизни " + standard);
        } catch (IllegalArgumentException e) {
            String availableValues = Arrays.stream(StandardOfLiving.values())
                    .map(Enum::toString)
                    .collect(Collectors.joining(", "));
            return new Response("ты такой уровень жизни вообще выдумал, вот доступные: " + availableValues);
        } catch (Exception e) {
            return new Response("что-то сломалось при удалении по уровню жизни: " + e.getMessage());
        }
    }
}