package com.example.geophotoapp;

public class Photo {
    private String path;
    private long id;

    public Photo(String path, long id) {
        this.path = path;
        this.id = id;
    }

    public String getPath() {
        return path;
    }

    public long getId() {
        return id;
    }
}
