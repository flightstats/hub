package com.flightstats.hub.model;


import com.flightstats.hub.util.TimeUtil;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;
import org.joda.time.DateTime;

@Builder
@Getter
@ToString
public class TimeQuery {
    private final String channelName;
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
        if (trace) {
            traces = new TracesImpl();
        } else {
            traces = Traces.NOOP;
        }
    }

}
