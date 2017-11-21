package com.flightstats.hub.model;

public enum Location {
    ALL,
    CACHE,
    CACHE_WRITE,
    CACHE_READ,
    LONG_TERM,
    LONG_TERM_SINGLE,
    LONG_TERM_BATCH;

    public static final String DEFAULT = "ALL";
}
