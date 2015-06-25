package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;
import lombok.experimental.Wither;
import org.joda.time.DateTime;

@Builder
@Getter
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TimeQuery {
    @Wither
    private final String channelName;
    private final String tagName;
    private final DateTime startTime;
    private final TimeUtil.Unit unit;
    private final Location location;
    private final boolean stable;
    private Traces traces;

    public Location getLocation() {
        if (location == null) {
            return Location.ALL;
        }
        return location;
    }

    public void trace(boolean trace) {
        traces = Traces.getTraces(trace);
    }

}
