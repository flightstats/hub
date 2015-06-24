package com.flightstats.hub.util;

import com.flightstats.hub.app.HubProperties;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
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

    public static DateTime time(boolean stable) {
        return stable ? stable() : now();
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

    static DateTime getBirthDay() {
        String property = HubProperties.getProperty("app.birthDay", "2015/01/01");
        return daysFormatter.parseDateTime(property);
    }

    public static DateTime getEarliestTime(int ttlDays) {
        DateTime limitTime = TimeUtil.now().minusDays((int) ttlDays);
        DateTime birthDay = TimeUtil.getBirthDay();
        if (limitTime.isBefore(birthDay)) {
            limitTime = birthDay;
        }
        return limitTime;
    }

    public enum Unit {
        MILLIS(millisFormatter, Duration.millis(1), "millis"),
        SECONDS(secondsFormatter, Duration.standardSeconds(1), "second"),
        MINUTES(minutesFormatter, Duration.standardMinutes(1), "minute"),
        HOURS(hoursFormatter, Duration.standardHours(1), "hour"),
        DAYS(daysFormatter, Duration.standardDays(1), "day");

        private DateTimeFormatter formatter;
        private Duration duration;
        private String name;

        Unit(DateTimeFormatter formatter, Duration duration, String name) {
            this.formatter = formatter;
            this.duration = duration;
            this.name = name;
        }

        public String format(DateTime dateTime) {
            return dateTime.toString(formatter);
        }

        public Duration getDuration() {
            return duration;
        }

        public String getName() {
            return name;
        }
    }

}
