package org.example.server;

import org.example.network.Request;
import org.example.network.Response;
import org.example.database.CollectionManager;

public interface ServerCommands {
    Response execute(Request request, CollectionManager collectionManager);
}