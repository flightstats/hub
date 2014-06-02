package com.flightstats.hub.model.exception;

public class ConflictException extends RuntimeException
{
	public ConflictException(String message)
	{
		super( message );
	}

	public ConflictException(String message, Throwable t)
	{
		super( message, t );
	}
}
