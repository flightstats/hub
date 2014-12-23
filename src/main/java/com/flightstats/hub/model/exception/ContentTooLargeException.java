package com.flightstats.hub.model.exception;

public class ContentTooLargeException extends RuntimeException {
    public ContentTooLargeException(String message) {
        super(message);
    }
}
