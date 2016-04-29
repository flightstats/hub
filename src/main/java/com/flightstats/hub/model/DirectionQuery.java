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
    private final Location location;
    private final boolean stable;
    @Wither
    private final long ttlDays;

    public Location getLocation() {
        if (location == null) {
            return Location.ALL;
        }
        return location;
    }

    @Override
    public boolean outsideOfCache(DateTime cacheTime) {
        if (next) {
            return contentKey.getTime().isBefore(cacheTime);
        }
        return true;
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
