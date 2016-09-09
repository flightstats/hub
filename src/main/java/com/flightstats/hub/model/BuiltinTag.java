package com.flightstats.hub.model;

public enum BuiltinTag {
    REPLICATED, GLOBAL, HISTORICAL;

    public String toString() {
        return this.name().toLowerCase();
    }
}
