package com.flightstats.hub.system.service;

import com.flightstats.hub.util.TimeUtil;
import lombok.Getter;
import org.joda.time.DateTime;

@Getter
class FormattedStringHelper {
    private String channelName;
    private String hashKey;
    private int year;
    private int month;
    private int day;
    private int hour;
    private int minute;
    private int second;
    private int millis;

    FormattedStringHelper withExtractedUrlPaths(String path, String baseUrl) {
        String trimmedPath = path.replace(baseUrl + "channel/", "");
        channelName = trimmedPath.substring(0, trimmedPath.indexOf("/"));
        hashKey = trimmedPath.substring(trimmedPath.lastIndexOf("/") + 1);
        String datePath = trimmedPath
                .replace(channelName, "")
                .replace(hashKey, "")
                .substring(1);
        DateTime dateTime = TimeUtil.millis(datePath);
        year = dateTime.getYear();
        month = dateTime.getMonthOfYear();
        day = dateTime.getDayOfMonth();
        hour = dateTime.getHourOfDay();
        minute = dateTime.getMinuteOfHour();
        second = dateTime.getSecondOfMinute();
        millis = dateTime.getMillisOfSecond();
        return this;
    }
}
