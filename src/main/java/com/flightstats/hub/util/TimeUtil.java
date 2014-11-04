package com.flightstats.hub.util;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter secondsFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ss/");
    private static final DateTimeFormatter minutesFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/");
    private static final DateTimeFormatter hoursFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/");

    public static DateTime now() {
        return new DateTime(DateTimeZone.UTC);
    }

    public static String secondsNow() {
        return now().toString(secondsFormatter);
    }

    public static String minutesNow() {
        return now().toString(minutesFormatter);
    }

    public static String hoursNow() {
        return now().toString(hoursFormatter);
    }

}
