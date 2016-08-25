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
    private final String tagName;
    @Wither
    private ContentKey contentKey;
    @Wither
    private final int count;
    private final boolean next;
    @Wither
    private final Location location;
    private final boolean stable;
    @Wither
    private DateTime ttlTime;
    @Wither
    private final boolean liveChannel;
    @Wither
    private final DateTime channelStable;

    public Location getLocation() {
        if (location == null) {
            return Location.ALL;
        }
        return location;
    }

    @Override
    public boolean outsideOfCache(DateTime cacheTime) {
        return !next || contentKey.getTime().isBefore(cacheTime);
    }

    @Override
    public String getUrlPath() {
        String direction = next ? "/next/" : "/previous/";
        return "/" + contentKey.toUrl() + direction + count + "?stable=" + stable;
    }

    public TimeQuery convert(DateTime startTime, TimeUtil.Unit unit) {
        return TimeQuery.builder().channelName(getChannelName())
                .startTime(startTime)
                .unit(unit)
                .limitKey(contentKey)
                .count(count)
                .build();
    }

}
