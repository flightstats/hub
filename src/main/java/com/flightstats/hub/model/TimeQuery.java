package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import lombok.*;
import lombok.experimental.Wither;
import org.joda.time.DateTime;

/**
 * If an endTime or limitKey is specified, the TimeQuery will move forward
 * by TimeUtil.Unit increments until the endTime or limitKey is reached.
 */
@Builder
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeQuery implements Query {
    @Wither
    private final String channelName;
    @Wither
    private final ChannelConfig channelConfig;
    private final String tagName;
    @Wither
    private final DateTime startTime;
    private final DateTime endTime;
    private final TimeUtil.Unit unit;
    @Wither
    private final Location location;
    private final boolean stable;
    private final int count;
    private final ContentKey limitKey;
    @Wither
    private final Epoch epoch;
    @Wither
    private final DateTime channelStable;

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

    public DateTime getEndTime() {
        if (endTime == null && limitKey != null) {
            return limitKey.getTime();
        }
        return endTime;
    }

    public boolean outsideOfCache(DateTime cacheTime) {
        return startTime.isBefore(cacheTime);
    }

    public String getUrlPath() {
        return "/" + getUnit().format(getStartTime()) + "?stable=" + stable;
    }
}
