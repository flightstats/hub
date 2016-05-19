package com.flightstats.hub.exception;

public class FailedQueryException extends RuntimeException {
    public FailedQueryException(String message) {
        super(message);
    }
}
