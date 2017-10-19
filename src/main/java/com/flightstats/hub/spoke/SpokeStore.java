package com.flightstats.hub.spoke;

public enum SpokeStore {
    WRITE,
    READ;

    public static SpokeStore from(String value) {
        return SpokeStore.valueOf(value.toUpperCase());
    }

    public String toString() {
        return name().toLowerCase();
    }
}
