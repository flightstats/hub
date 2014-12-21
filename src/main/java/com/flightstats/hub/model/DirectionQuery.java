package com.flightstats.hub.model;


import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;

@Builder
@Getter
@ToString
public class DirectionQuery {
    private final String channelName;
    private final ContentKey contentKey;
    private final int count;
    private final boolean next;
    private final Location location;
    private final boolean stable;
    private final long ttlDays;

    public Location getLocation() {
        if (location == null) {
            return Location.ALL;
        }
        return location;
    }

    public long getTtlDays() {
        if (ttlDays == 0) {
            return 1;
        }
        return ttlDays;
    }
}
