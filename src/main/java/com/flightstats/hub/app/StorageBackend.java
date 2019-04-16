package com.flightstats.hub.app;

public enum StorageBackend {
    AWS("aws"),
    NAS("nas"),
    TEST("test");

    private final String name;

    StorageBackend(String name) {
        this.name = name;
    }

    String getName() { return name; }
}
