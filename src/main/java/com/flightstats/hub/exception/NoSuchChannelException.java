package com.flightstats.hub.exception;

public class NoSuchChannelException extends RuntimeException {
    public NoSuchChannelException(String message) {
        super(message);
    }

    public NoSuchChannelException(String message, Exception e) {
        super(message, e);
    }
}
