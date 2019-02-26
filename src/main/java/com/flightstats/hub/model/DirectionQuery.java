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
    @Wither
    private final String tagName;
    @Wither
    private final int count;
    @Wither
    private final boolean next;
    @Wither
    private final Location location;
    @Wither
    private final boolean stable;
    @Wither
    private final DateTime channelStable;
    @Wither
    private final Epoch epoch;
    /**
     * The startKey is exclusive.
     */
    @Wither
    private ContentKey startKey;
    private boolean inclusive;
    /**
     * earliestTime is only relevant for previous queries.
     */
    @Wither
    private DateTime earliestTime;

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

    public TimeQuery.TimeQueryBuilder convert(TimeUtil.Unit unit) {
        return TimeQuery.builder().channelName(getChannelName())
                .startTime(startKey.getTime())
                .unit(unit)
                .limitKey(startKey)
                .count(count)
                .epoch(epoch);
    }

    public TimeQuery convert(DateTime startTime, TimeUtil.Unit unit) {
        return convert(unit).startTime(startTime).build();
    }

}
