package com.flightstats.datahub.dao.dynamo;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

/**
 *
 */
public class TimeSeriesHashStamp {

    static DateTimeFormatter formatter = DateTimeFormat.forPattern("yyyy-MM-dd-HH-mmZ");
    static DateTimeZone timeZone = DateTimeZone.forID("America/Los_Angeles");

    public static String getHashStamp(DateTime dateTime) {
        return formatter.print(dateTime.withZone(timeZone));
    }

    public static String getHashStamp() {
        return getHashStamp(new DateTime());
    }

}
