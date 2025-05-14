package org.example.collection;

import com.google.gson.*;
import org.example.collection.models.City;
import org.example.collection.exceptions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DumpManager {

    public static JsonElement readJson(String filePath) throws IOException {
        if (!Files.exists(Path.of(filePath))) {
            if (!Files.exists(Path.of(filePath + ".json"))) {
                throw new IOException("такого файла нет");
            } else {
                filePath = filePath + ".json";
            }
        }

        String jsonString = Files.readString(Path.of(filePath));

        if (jsonString.isEmpty()) {
            return null;
        }

        return JsonParser.parseString(jsonString);
    }

    public static void saveJson(String jsonString, String filePath) throws IOException {
        if (!Files.exists(Path.of(filePath))) {
            if (!Files.exists(Path.of(filePath + ".json"))) {
                Files.createFile(Path.of(filePath));
            } else {
                filePath = filePath + ".json";
            }
        }
        Files.writeString(Path.of(filePath), jsonString);
    }

    // метод для преобразования JSON в список городов
    public static List<City> jsonFileToCityList(String filePath) throws IOException {
        JsonElement jsonElement = readJson(filePath);
        List<City> cityList = new ArrayList<>();
        Gson gson = new GsonBuilder().create();

        if (jsonElement == null || jsonElement.isJsonNull()) {
            System.out.println("файл пустой введи insert чтобы добавить новый город");
            return cityList;
        }

        if (jsonElement.isJsonArray()) {
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (JsonElement element : jsonArray.asList()) {
                City city = gson.fromJson(element, City.class);
                if (city == null) {
                    throw new IOException("ошибка: некорректные данные в файле объект City равен null");
                }
                cityList.add(city);
            }
        } else {
            City city = gson.fromJson(jsonElement, City.class);
            if (city == null) {
                throw new IOException("ошибка: некорректные данные в файле объект City равен null");
            }
            cityList.add(city);
        }

        return cityList;
    }

    // метод для сохранения коллекции в файл JSON
    public static void CollectionToJsonFile(Collection collection, String filePath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            saveJson(gson.toJson(collection), filePath);
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
    }
    //метод для получения минимального свободного положительного ID
    public static int getSmallestAvailableId(Collection<Integer> existingIds) {
        int id = 1;
        while (existingIds.contains(id)) {
            id++;
        }
        return id;
    }

}