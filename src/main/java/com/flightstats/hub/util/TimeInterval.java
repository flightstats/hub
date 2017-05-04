package com.flightstats.hub.util;

import org.joda.time.DateTime;
import org.joda.time.Interval;

/**
 * TimeInterval differs from Joda's Interval in that a null endTime is always 'now'.
 * Interval locks 'now' to be the construction time of the object.
 * TimeInterval is also always inclusive, while Interval in exclusive on the end point.
 */
public class TimeInterval {

    private DateTime startTime;
    private DateTime endTime;

    TimeInterval(DateTime startTime) {
        this(startTime, null);
    }

    public TimeInterval(DateTime startTime, DateTime endTime) {
        this.startTime = startTime;
        if (endTime != null) {
            this.endTime = endTime.plusMillis(1);
        }
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
        //adding a millisecond to make the end also inclusive
        return new Interval(startTime, endTime).overlaps(new Interval(start, end.plusMillis(1)));
    }

    @Override
    public String toString() {
        return "TimeInterval{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }

    public boolean isAfter(DateTime pointInTime) {
        if (startTime.isAfter(pointInTime)) {
            return true;
        }
        return contains(pointInTime);
    }
}
