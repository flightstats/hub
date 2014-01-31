package com.flightstats.hub.dao.timeIndex;

import com.flightstats.hub.model.ContentKey;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 */
public class TimeIndex {

    public static final String PATTERN = "yyyy-MM-dd'T'HH:mmZ";
    private static final DateTimeFormatter formatter = DateTimeFormat.forPattern(PATTERN);
    private static final DateTimeZone timeZone = DateTimeZone.forID("UTC");
    private static final String TIME_INDEX = "/TimeIndex";

    public static String getHash(DateTime dateTime) {
        return formatter.print(dateTime.withZone(timeZone));
    }

    public static DateTime parseHash(String dateTime) {
        return formatter.parseDateTime(dateTime);
    }

    public static String getPath() {
        return TIME_INDEX;
    }

    public static String getPath(String channel) {
        return getPath() + "/" + channel;
    }

    public static String getPath(String channel, DateTime dateTime, ContentKey key) {
        return getPath(channel, getHash(dateTime)) + "/" + key.keyToString();
    }

    public static String getPath(String channel, String dateTime) {
        return getPath(channel) + "/" + dateTime;
    }

}
