package com.flightstats.hub.exception;

public class ForbiddenRequestException extends RuntimeException
{
    public ForbiddenRequestException(String message)
    {
        super( message );
    }

    public ForbiddenRequestException(String message, Throwable t)
    {
        super( message, t );
    }
}