package com.flightstats.hub.model;

public enum DatePathIndex {
    YEAR(0),
    MONTH(1),
    DAY(2),
    HOUR(3),
    MINUTE(4),
    SECONDS(5),
    MILLIS(6);

    private int index;

    DatePathIndex(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
