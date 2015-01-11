package com.flightstats.hub.exception;

public class InvalidRequestException extends RuntimeException
{
	public InvalidRequestException( String message )
	{
		super( message );
	}

	public InvalidRequestException( String message, Throwable t )
	{
		super( message, t );
	}
}
