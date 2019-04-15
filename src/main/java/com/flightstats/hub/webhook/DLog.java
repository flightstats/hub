package com.flightstats.hub.webhook;

public class DLog {

    public static void log(String message) {
        System.out.println("Damon - " + message + " (" + Thread.currentThread().getName() + ")");
    }
}
