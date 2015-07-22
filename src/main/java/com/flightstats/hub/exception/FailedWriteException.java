package com.flightstats.hub.exception;

public class FailedWriteException extends RuntimeException {
    public FailedWriteException(String message) {
        super(message);
    }

    public FailedWriteException(String message, Throwable t) {
        super(message, t);
    }
}
