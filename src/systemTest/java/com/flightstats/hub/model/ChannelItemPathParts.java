package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Getter;
import org.joda.time.DateTime;

@Getter
@Builder
public class ChannelItemPathParts {
    String path;
    String timePath;

    String channelName;
    String hashKey;
    int year;
    int month;
    int day;
    int hour;
    int minute;
    int second;
    int millis;

    DateTime dateTime;
}
