package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.joda.time.DateTime;

/**
 * If an endTime or limitKey is specified, the TimeQuery will move forward
 * by TimeUtil.Unit increments until the endTime or limitKey is reached.
 */
@Builder
@Wither
@Value
public class TimeQuery implements Query {
    String channelName;
    ChannelConfig channelConfig;
    String tagName;
    DateTime startTime;
    TimeUtil.Unit unit;
    Location location;
    boolean stable;
    int count;
    ContentKey limitKey;
    Epoch epoch;
    DateTime channelStable;

    public Location getLocation() {
        if (location == null) {
            return Location.ALL;
        }
        return location;
    }

    public Epoch getEpoch() {
        if (epoch == null) {
            return Epoch.IMMUTABLE;
        }
        return epoch;
    }

    public boolean outsideOfCache(DateTime cacheTime) {
        return startTime.isBefore(cacheTime) || startTime.isEqual(cacheTime);
    }

    public String getUrlPath() {
        return "/" + getUnit().format(getStartTime()) + "?stable=" + stable;
    }
}
