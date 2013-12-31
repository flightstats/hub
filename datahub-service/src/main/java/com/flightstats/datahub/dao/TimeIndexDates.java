package com.flightstats.datahub.dao;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 */
public class TimeIndexDates {

    public static final String PATTERN = "yyyy-MM-dd'T'HH:mmZ";
    private static DateTimeFormatter formatter = DateTimeFormat.forPattern(PATTERN);
    private static DateTimeZone timeZone = DateTimeZone.forID("America/Los_Angeles");

    public static String getString(DateTime dateTime) {
        return formatter.print(dateTime.withZone(timeZone));
    }

    public static String getString() {
        return getString(new DateTime());
    }

    public static DateTime parse(String dateTime) {
        return formatter.parseDateTime(dateTime);
    }

}
