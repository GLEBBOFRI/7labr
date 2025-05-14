package org.example.collection;

import org.example.collection.exceptions.ValidationException;
import org.example.collection.models.City;
import org.example.collection.models.StandardOfLiving;
import org.example.utils.IdGenerator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class CollectionManager {
    private final Hashtable<Integer, City> collection = new Hashtable<>();
    private final IdGenerator idGenerator = new IdGenerator();
    private String saveFilePath;
    private final ExecutorService saveExecutor = Executors.newSingleThreadExecutor(); // Один поток для сохранения

    public void loadCollection(String filePath) throws IOException, ValidationException {
        this.saveFilePath = filePath;
        List<City> cities = DumpManager.jsonFileToCityList(filePath);

        collection.clear();
        idGenerator.setNextId(1);

        for (City city : cities) {
            city.validate();
            collection.put(city.getId(), city);
            idGenerator.setNextId(city.getId() + 1);
        }
        asyncSave();
    }

    private void asyncSave() {
        if (saveFilePath != null) {
            saveExecutor.submit(() -> {
                try {
                    DumpManager.CollectionToJsonFile(new ArrayList<>(collection.values()), saveFilePath);
                } catch (IOException e) {
                    System.err.println("ошибка при асинхронном сохранении: " + e.getMessage());
                }
            });
        }
    }

    public void shutdownSaveExecutor() {
        saveExecutor.shutdown();
    }

    public void addElement(Integer key, City city) throws ValidationException, IOException {
        int newId = idGenerator.getNextId();
        city.setId(newId);

        city.validate();

        int actualKey = (key != null) ? key : newId;
        collection.put(actualKey, city);
        asyncSave(); 
    }

    public boolean update(Integer key, City newCity) throws ValidationException, IOException {
        if (!collection.containsKey(key)) {
            return false;
        }

        City oldCity = collection.get(key);
        newCity.setId(oldCity.getId());
        collection.put(key, newCity);
        asyncSave(); 
        return true;
    }

    public boolean remove(Integer key) throws IOException {
        boolean removed = collection.remove(key) != null;
        if (removed) {
            asyncSave(); 
        }
        return removed;
    }

    public int removeGreaterKey(Integer key) throws IOException {
        int initialSize = collection.size();
        collection.keySet().removeIf(k -> k > key);
        int removed = initialSize - collection.size();
        if (removed > 0) {
            asyncSave(); 
        }
        return removed;
    }

    public int removeLowerKey(Integer key) throws IOException {
        int initialSize = collection.size();
        collection.keySet().removeIf(k -> k < key);
        int removed = initialSize - collection.size();
        if (removed > 0) {
            asyncSave(); 
        }
        return removed;
    }

    public int removeAllByStandardOfLiving(StandardOfLiving standard) throws IOException {
        int initialSize = collection.size();
        collection.values().removeIf(city -> standard.equals(city.getStandardOfLiving()));
        int removed = initialSize - collection.size();
        if (removed > 0) {
            asyncSave(); 
        }
        return removed;
    }

    public void clearCollection() throws IOException {
        collection.clear();
        asyncSave(); 
    }

    public boolean replaceIfGreater(Integer key, City newCity) throws ValidationException, IOException {
        if (!collection.containsKey(key)) {
            return false;
        }

        City oldCity = collection.get(key);
        if (newCity.getPopulation() > oldCity.getPopulation()) {
            newCity.setId(oldCity.getId());
            newCity.validate();
            collection.put(key, newCity);
            asyncSave(); 
            return true;
        }
        return false;
    }

    public String getCollectionInfo() {
        return String.format(
                "тип: %s\nразмер: %d\nфайл сохранения: %s",
                collection.getClass().getName(),
                collection.size(),
                saveFilePath != null ? saveFilePath : "не указан"
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
}