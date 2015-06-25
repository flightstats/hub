package com.flightstats.hub.model;


import lombok.*;
import lombok.experimental.Builder;
import lombok.experimental.Wither;

@Builder
@Getter
@ToString(exclude = {"traces"})
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DirectionQuery {
    @Wither
    private final String channelName;
    private final String tagName;
    @Setter
    private ContentKey contentKey;
    private final int count;
    private final boolean next;
    private final Location location;
    private final boolean stable;
    private final long ttlDays;
    @Setter
    private Traces traces;

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

    public Traces getTraces() {
        if (traces == null) {
            return Traces.NOOP;
        }
        return traces;
    }

    public void trace(boolean trace) {
        traces = Traces.getTraces(trace);
    }
}
