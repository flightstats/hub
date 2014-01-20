package com.flightstats.datahub.util;

/**
 *
 */
public class Sleeper
{
    public static void sleep(long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeInterruptedException(e);
        }
    }
}