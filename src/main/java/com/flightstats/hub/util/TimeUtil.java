package com.flightstats.hub.util;

import com.flightstats.hub.app.HubProperties;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class TimeUtil {
    private static final DateTimeFormatter millisFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ss/SSS/").withZoneUTC();
    private static final DateTimeFormatter secondsFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ss").withZoneUTC();
    private static final DateTimeFormatter minutesFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm").withZoneUTC();
    private static final DateTimeFormatter hoursFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH").withZoneUTC();
    private static final DateTimeFormatter daysFormatter = DateTimeFormat.forPattern("yyyy/MM/dd").withZoneUTC();

    static {
        stableSeconds = HubProperties.getProperty("app.stable_seconds", 5);
    }

    private static final int stableSeconds;

    public static DateTime now() {
        return new DateTime(DateTimeZone.UTC);
    }

    public static DateTime stable() {
        return now().minusSeconds(stableSeconds).withMillisOfSecond(0);
    }

    public static String seconds(DateTime dateTime) {
        return dateTime.toString(secondsFormatter);
    }

    public static String millis(DateTime dateTime) {
        return dateTime.toString(millisFormatter);
    }

    public static DateTime millis(String string) {
        return millisFormatter.parseDateTime(string);
    }

    public static String minutes(DateTime dateTime) {
        return dateTime.toString(minutesFormatter);
    }

    public static String hours(DateTime dateTime) {
        return dateTime.toString(hoursFormatter);
    }

    public static String days(DateTime dateTime) {
        return dateTime.toString(daysFormatter);
    }

    public enum Unit {
        MILLIS(millisFormatter),
        SECONDS(secondsFormatter),
        MINUTES(minutesFormatter),
        HOURS(hoursFormatter),
        DAYS(daysFormatter);

        private DateTimeFormatter formatter;

        Unit(DateTimeFormatter formatter) {
            this.formatter = formatter;
        }

        public String format(DateTime dateTime) {
            return dateTime.toString(formatter);
        }
    }

}
