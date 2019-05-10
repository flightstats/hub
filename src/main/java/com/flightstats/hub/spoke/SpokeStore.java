package com.flightstats.hub.spoke;

public enum SpokeStore {
    WRITE,
    READ;
    // Eww...but attribute values in an @Named have to be constant.
    public static final String WRITE_SPOKE_NAME = "WRITE";
    public static final String READ_SPOKE_NAME = "READ";

    public static SpokeStore from(String value) {
        return SpokeStore.valueOf(value.toUpperCase());
    }

    public String toString() {
        return name().toLowerCase();
    }
}
