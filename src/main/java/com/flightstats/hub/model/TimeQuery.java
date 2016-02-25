package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import lombok.*;
import lombok.experimental.Wither;
import org.joda.time.DateTime;

@Builder
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeQuery implements Query {
    @Wither
    private final String channelName;
    private final String tagName;
    @Wither
    private final DateTime startTime;
    private final DateTime endTime;
    private final TimeUtil.Unit unit;
    private final Location location;
    private final boolean stable;
    private final int count;
    private final ContentKey limitKey;

    public Location getLocation() {
        if (location == null) {
            return Location.ALL;
        }
        return location;
    }

    public DateTime getEndTime() {
        if (endTime == null && limitKey != null) {
            return limitKey.getTime();
        }
        return endTime;
    }
}
