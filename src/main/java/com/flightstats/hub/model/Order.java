package com.flightstats.hub.model;

public enum Order {
    ASCENDING,
    DESCENDING;

    public static final String DEFAULT = "ASCENDING";

    public static boolean isDescending(String value) {
        return value.toLowerCase().startsWith("d");
    }

}
