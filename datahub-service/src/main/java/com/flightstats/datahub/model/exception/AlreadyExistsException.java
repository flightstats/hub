package com.flightstats.datahub.model.exception;

public class AlreadyExistsException extends Exception
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
