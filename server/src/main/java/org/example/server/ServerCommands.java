package org.example.server;

import org.example.network.Request;
import org.example.network.Response;
import org.example.collection.CollectionManager;

public interface ServerCommands {
    Response execute(Request request, CollectionManager collectionManager);
}