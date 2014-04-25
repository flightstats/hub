package com.flightstats.hub.model.exception;

public class ReplicatingChannelException extends RuntimeException
{
    public ReplicatingChannelException(String message)
    {
        super( message );
    }

    public ReplicatingChannelException(String message, Throwable t)
    {
        super( message, t );
    }
}