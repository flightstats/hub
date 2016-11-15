package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import lombok.*;
import lombok.experimental.Wither;
import org.joda.time.DateTime;

@Builder
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DirectionQuery implements Query {
    @Wither
    private final String channelName;
    @Wither
    private final ChannelConfig channelConfig;
    private final String tagName;
    @Wither
    private ContentKey startKey;
    @Wither
    private final int count;
    private final boolean next;
    @Wither
    private final Location location;
    private final boolean stable;

    /**
     * earliestTime is only relevant for previous queries.
     */
    @Wither
    private DateTime earliestTime;
    @Wither
    private final boolean liveChannel;
    /**
     * this is only used by Spoke.query(direction)
     */
    @Wither
    private final DateTime channelStable;

    @Wither
    private final Epoch epoch;

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

    @Override
    public boolean outsideOfCache(DateTime cacheTime) {
        return !next || startKey.getTime().isBefore(cacheTime);
    }

    @Override
    public String getUrlPath() {
        String direction = next ? "/next/" : "/previous/";
        return "/" + startKey.toUrl() + direction + count + "?stable=" + stable;
    }

    public TimeQuery convert(DateTime startTime, TimeUtil.Unit unit) {
        return TimeQuery.builder().channelName(getChannelName())
                .startTime(startTime)
                .unit(unit)
                .limitKey(startKey)
                .count(count)
                .epoch(epoch)
                .build();
    }

}
