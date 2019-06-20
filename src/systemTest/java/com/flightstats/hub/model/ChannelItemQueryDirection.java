package com.flightstats.hub.model;

public enum ChannelItemQueryDirection {
    NEXT,
    PREVIOUS,
    EARLIEST,
    LATEST;

    public String toString() {
        return name().toLowerCase();
    }
}
