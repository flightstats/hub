package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;
import org.joda.time.DateTime;

@Builder
@Getter
@ToString
public class TimeQuery {
    private final String channelName;
    private final DateTime startTime;
    private final TimeUtil.Unit unit;
    private Location location = Location.ALL;

    public enum Location {
        ALL,
        CACHE,
        LONG_TERM
    }
}
