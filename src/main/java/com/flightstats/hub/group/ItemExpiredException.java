package com.flightstats.hub.group;

public class ItemExpiredException extends RuntimeException {
    public ItemExpiredException(String message) {
        super(message);
    }
}
