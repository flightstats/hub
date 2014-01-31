package com.flightstats.hub.model.exception;

public class AlreadyExistsException extends RuntimeException
{
	public AlreadyExistsException( String message )
	{
		super( message );
	}

	public AlreadyExistsException( String message, Throwable t )
	{
		super( message, t );
	}
}
