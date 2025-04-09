package org.example.collection.utils;

public class CollectionInfo {
    private final String collectionType;
    private final int size;
    private final String loadedFrom;

    public CollectionInfo(String collectionType, int size, String loadedFrom) {
        this.collectionType = collectionType;
        this.size = size;
        this.loadedFrom = loadedFrom;
    }

    public String getCollectionType() {
        return collectionType;
    }

    public int getSize() {
        return size;
    }

    public String getLoadedFrom() {
        return loadedFrom;
    }
}