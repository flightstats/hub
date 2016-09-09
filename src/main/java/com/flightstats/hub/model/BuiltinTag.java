package com.flightstats.hub.model;

public enum BuiltInTag {
    REPLICATED, GLOBAL, HISTORICAL;

    public String toString() {
        return this.name().toLowerCase();
    }
}
