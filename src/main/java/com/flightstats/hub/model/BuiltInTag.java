package com.flightstats.hub.model;

public enum BuiltInTag {
    REPLICATED, HISTORICAL;

    public String toString() {
        return this.name().toLowerCase();
    }
}
