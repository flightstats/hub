package com.flightstats.hub.exception;

public class FailedReadException extends RuntimeException {
    public FailedReadException(String message) {
        super(message);
    }

    public FailedReadException(String message, Throwable t) {
        super(message, t);
    }
}
