package com.flightstats.hub.webhook;

class ItemExpiredException extends RuntimeException {
    ItemExpiredException(String message) {
        super(message);
    }
}
