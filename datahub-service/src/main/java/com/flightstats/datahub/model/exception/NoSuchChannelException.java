package com.flightstats.datahub.model.exception;

public class NoSuchChannelException extends RuntimeException {
	public NoSuchChannelException(String message, Exception e) {
		super(message, e);
	}
}
