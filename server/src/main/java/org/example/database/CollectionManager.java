package org.example.database;

import org.example.database.exceptions.ValidationException;
import org.example.database.models.City;
import org.example.database.models.StandardOfLiving;
import org.example.database.DatabaseManager;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class CollectionManager {
    private final Hashtable<Integer, City> collection = new Hashtable<>();
    private final Date initializationDate;
    private final DatabaseManager databaseManager;

    public CollectionManager(DatabaseManager databaseManager) {
        this.initializationDate = new Date();
        this.databaseManager = databaseManager;
        try {
            loadCollectionFromDb();
        } catch (SQLException e) {
            System.err.println("Ошибка при загрузке коллекции из базы данных при запуске: " + e.getMessage());
        }
    }

    public void loadCollectionFromDb() throws SQLException {
        collection.clear();
        Hashtable<Integer, City> loadedCities = databaseManager.loadCities();
        collection.putAll(loadedCities);
        System.out.println("Коллекция успешно загружена из базы данных. Количество элементов: " + collection.size());
    }

    public boolean addElement(City city, String ownerId) throws ValidationException, SQLException {
        city.validate();

        int newId = databaseManager.insertCity(city, ownerId);
        city.setId(newId);
        city.setOwnerId(ownerId);

        collection.put(newId, city);
        return true;
    }

    public boolean update(Integer id, City newCity, String ownerId) throws ValidationException, SQLException {
        if (!collection.containsKey(id)) {
            return false;
        }
        newCity.setId(id);
        newCity.setOwnerId(ownerId);
        newCity.validate();

        boolean dbUpdated = databaseManager.updateCity(newCity, ownerId);
        if (dbUpdated) {
            collection.put(id, newCity);
            return true;
        }
        return false;
    }

    public boolean remove(Integer id, String ownerId) throws SQLException {
        if (!collection.containsKey(id)) {
            return false;
        }
        boolean dbRemoved = databaseManager.deleteCity(id, ownerId);
        if (dbRemoved) {
            collection.remove(id);
            return true;
        }
        return false;
    }

    public int removeGreaterKey(Integer key, String ownerId) throws SQLException {
        int removedCount = databaseManager.deleteCitiesGreaterThanKey(key, ownerId);
        if (removedCount > 0) {
            loadCollectionFromDb();
        }
        return removedCount;
    }

    public int removeLowerKey(Integer key, String ownerId) throws SQLException {
        int removedCount = databaseManager.deleteCitiesLowerThanKey(key, ownerId);
        if (removedCount > 0) {
            loadCollectionFromDb();
        }
        return removedCount;
    }

    public int removeAllByStandardOfLiving(StandardOfLiving standard, String ownerId) throws SQLException {
        int removedCount = databaseManager.deleteCitiesByStandardOfLiving(standard, ownerId);
        if (removedCount > 0) {
            loadCollectionFromDb();
        }
        return removedCount;
    }

    public int clearCollection(String ownerId) throws SQLException {
        int removedCount = databaseManager.clearCities(ownerId);
        if (removedCount > 0) {
            loadCollectionFromDb();
        }
        return removedCount;
    }

    public boolean replaceIfGreater(Integer key, City newCity, String ownerId) throws ValidationException, SQLException {
        if (!collection.containsKey(key)) {
            return false;
        }
        City oldCity = collection.get(key);
        if (newCity.getPopulation() > oldCity.getPopulation()) {
            newCity.setId(oldCity.getId());
            newCity.setOwnerId(ownerId);
            newCity.validate();

            boolean dbUpdated = databaseManager.updateCity(newCity, ownerId);
            if (dbUpdated) {
                collection.put(key, newCity);
                return true;
            }
        }
        return false;
    }

    public String getCollectionInfo() {
        return String.format(
                "Тип: %s\nДата инициализации: %s\nКоличество элементов: %d",
                collection.getClass().getName(),
                initializationDate.toString(),
                collection.size()
        );
    }

    public Collection<City> getSortedCollection() {
        return collection.values().stream()
                .sorted()
                .collect(Collectors.toList());
    }

    public List<City> filterStartsWithName(String prefix) {
        return collection.values().stream()
                .filter(city -> city.getName().startsWith(prefix))
                .collect(Collectors.toList());
    }

    public double getAverageMetersAboveSeaLevel() {
        return collection.values().stream()
                .filter(city -> city.getMetersAboveSeaLevel() != null)
                .mapToDouble(City::getMetersAboveSeaLevel)
                .average()
                .orElse(0.0);
    }

    public Hashtable<Integer, City> getCollection() {
        return collection;
    }

    public boolean containsKey(Integer key) {
        return collection.containsKey(key);
    }

    public Set<Integer> getKeys() {
        return collection.keySet();
    }

    public void closeDatabaseConnection() {
        databaseManager.closeConnection();
    }
}
