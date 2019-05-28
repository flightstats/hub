package com.flightstats.hub.util;

import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.ContentProperties;
import com.flightstats.hub.config.properties.PropertiesLoader;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.joda.time.Seconds;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class TimeUtil {

    private static final AppProperties appProperties = new AppProperties(PropertiesLoader.getInstance());
    private static final ContentProperties contentProperties = new ContentProperties(PropertiesLoader.getInstance());
    public static final DateTimeFormatter FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();
    public static final DateTime BIG_BANG = new DateTime(1, DateTimeZone.UTC);

    private static final DateTimeFormatter millisFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ss/SSS/").withZoneUTC();
    private static final DateTimeFormatter secondsFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ss").withZoneUTC();
    private static final DateTimeFormatter minutesFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm").withZoneUTC();
    private static final DateTimeFormatter hoursFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH").withZoneUTC();
    private static final DateTimeFormatter daysFormatter = DateTimeFormat.forPattern("yyyy/MM/dd").withZoneUTC();
    private static final DateTimeFormatter monthsFormatter = DateTimeFormat.forPattern("yyyy/MM").withZoneUTC();
    private static final int stableSeconds;

    static {
        stableSeconds = contentProperties.getStableSeconds();
    }

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

    public static DateTime seconds(String string) {
        return secondsFormatter.parseDateTime(string);
    }

    public static String millis(DateTime dateTime) {
        return dateTime.toString(millisFormatter);
    }

    public static DateTime millis(String string) {
        return millisFormatter.parseDateTime(string);
    }

    public static DateTime minutes(String string) {
        return minutesFormatter.parseDateTime(string);
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

    public static String months(DateTime dateTime) {
        return dateTime.toString(monthsFormatter);
    }

    static DateTime getBirthDay() {
        String property = appProperties.getAppBirthday();
        return daysFormatter.parseDateTime(property);
    }

    public static DateTime getEarliestTime(long ttlDays) {
        DateTime birthDay = TimeUtil.getBirthDay();
        if (ttlDays == 0) {
            return birthDay;
        }
        DateTime limitTime = TimeUtil.now().minusDays((int) ttlDays);
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
        DAYS(daysFormatter, Duration.standardDays(1), "day"),
        MONTHS(monthsFormatter, Duration.standardDays(28), "months");

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

        public DateTime round(DateTime time) {
            DateTime rounded = time;
            if (name.equals("millis")) {
                return rounded;
            }
            rounded = rounded.withMillisOfSecond(0);
            if (name.equals("second")) {
                return rounded;
            }
            rounded = rounded.withSecondOfMinute(0);
            if (name.equals("minute")) {
                return rounded;
            }
            rounded = rounded.withMinuteOfHour(0);
            if (name.equals("hour")) {
                return rounded;
            }
            return rounded.withHourOfDay(0);
        }

        public boolean lessThanOrEqual(Unit unit) {
            Seconds otherSeconds = unit.getDuration().toStandardSeconds();
            return getDuration().toStandardSeconds().getSeconds() <= otherSeconds.getSeconds();
        }
    }

}
