package com.flightstats.hub.util;

import org.joda.time.DateTime;
import org.joda.time.Interval;

public class TimeInterval {

    private DateTime startTime;
    private DateTime endTime;

    TimeInterval(DateTime startTime) {
        this(startTime, null);
    }

    public TimeInterval(DateTime startTime, DateTime endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public boolean contains(DateTime pointInTime) {
        if (endTime == null) {
            return !pointInTime.isBefore(startTime);
        }
        return new Interval(startTime, endTime).contains(pointInTime);
    }

    public boolean overlaps(DateTime start, DateTime end) {
        if (endTime == null) {
            if (end == null || end.isAfterNow()) {
                return true;
            }
        }
        return new Interval(startTime, endTime).overlaps(new Interval(start, end));

    }
}
