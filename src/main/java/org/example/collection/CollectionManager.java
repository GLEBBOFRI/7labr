package org.example.collection;

import org.example.collection.models.City;
import org.example.collection.exceptions.ValidationException;
import org.example.collection.utils.CollectionInfo;
import org.example.collection.utils.IdGenerator;

import java.io.IOException;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

public class CollectionManager {
    private CollectionInfo collectionInfo = new CollectionInfo(null, 0, null);
    private final IdGenerator idGenerator = new IdGenerator(); // Генератор ID
    private final Hashtable<Integer, City> collection = new Hashtable<>();

    // метод для обновления информации о коллекции
    public void updateCollectionInfo() {
        this.collectionInfo = new CollectionInfo(
                collection.getClass().getName(),
                collection.size(),
                collectionInfo.getLoadedFrom()
        );
    }

    // перезагрузка коллекции из файла
    public void reloadCollectionFromFile() throws IOException, ValidationException {
        String filePath = collectionInfo.getLoadedFrom();
        if (filePath == null) {
            throw new IOException("файл инициализации не задан");
        }

        // очищаем коллекцию
        collection.clear();

        // загружаем коллекцию и синхронизируем генератор ID
        loadCollection(filePath);
    }

    // загрузка коллекции из файла
    public void loadCollection(String filePath) throws IOException, ValidationException {
        List<City> cityList = DumpManager.jsonFileToCityList(filePath);
        if (cityList.isEmpty()) {
            System.out.println("файл пустой введи insert, чтобы добавить город");
        } else {
            long maxId = 0; // для синхронизации генератора ID
            for (City city : cityList) {
                if (city == null) {
                    throw new ValidationException("ошибка: Некорректные данные в файле объект City равен null");
                }
                // добавляем город в коллекцию с сохранением ID
                this.collection.put(cityList.indexOf(city), city);
                if (city.getId() > maxId) {
                    maxId = city.getId();
                }
            }
            idGenerator.setNextId(maxId + 1);
        }
        this.collectionInfo = new CollectionInfo(collection.getClass().getName(), collection.size(), filePath);
    }

    // метод добавления нового города в коллекцию
    public void add(Integer key, City city) throws ValidationException {
        if (city == null) {
            throw new ValidationException("ошибка: переданный объект City равен null");
        }

        // генерация уникального ID
        Long id = idGenerator.getNextId();
        city.setId(id);

        // проверка и добавление объекта
        city.validate();
        if (city.getCoordinates() != null) city.getCoordinates().validate();
        if (city.getGovernor() != null) city.getGovernor().validate();

        collection.put(key, city);
    }

    // проверка, является ли ID свободным
    public boolean isIdFree(Long id) {
        for (City city : collection.values()) {
            if (city.getId().equals(id)) return false;
        }
        return true;
    }

    // метод для обновления существующего города
    public void update(Integer key, City city) throws ValidationException {
        if (!collection.containsKey(key)) {
            throw new ValidationException("элемент с ключом " + key + " не найден");
        }

        City existingCity = collection.get(key);
        city.setId(existingCity.getId());

        city.validate();
        if (city.getCoordinates() != null) city.getCoordinates().validate();
        if (city.getGovernor() != null) city.getGovernor().validate();

        collection.put(key, city);
    }

    // удаление города по ключу
    public boolean remove(Integer key) {
        return collection.remove(key) != null;
    }

    // удаление города по ID
    public boolean removeById(Long id) {
        return collection.values().removeIf(city -> city.getId().equals(id));
    }

    // очистка всей коллекции
    public boolean removeAll() {
        collection.clear();
        return true;
    }

    // сохранение коллекции в файл
    public void saveCollection(String filePath) throws IOException {
        DumpManager.CollectionToJsonFile(collection.values(), filePath);
    }

    // получение информации о коллекции
    public CollectionInfo getCollectionInfo() {
        return collectionInfo;
    }

    // доступ к всей коллекции
    public Hashtable<Integer, City> getCollection() {
        return collection;
    }

    // переопределение toString для вывода объектов коллекции
    @Override
    public String toString() {
        if (collection.isEmpty()) {
            return "коллекция пуста";
        }
        StringBuilder string = new StringBuilder();
        for (City city : collection.values()) {
            string.append(city.toString()).append("\n");
        }
        return string.toString().trim();
    }

    // получение всех ключей коллекции
    public Set<Integer> getKeys() {
        return collection.keySet();
    }
}
