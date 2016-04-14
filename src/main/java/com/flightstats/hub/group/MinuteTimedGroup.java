package com.flightstats.hub.group;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.ContentPathKeys;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.Collection;

public class MinuteTimedGroup implements TimedGroup {

    public static final MinuteTimedGroup GROUP = new MinuteTimedGroup();

    @Override
    public int getOffsetSeconds() {
        int secondOfMinute = new DateTime().getSecondOfMinute();
        if (secondOfMinute < 6) {
            return 6 - secondOfMinute;
        } else if (secondOfMinute > 6) {
            return 66 - secondOfMinute;
        }
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 60;
    }

    @Override
    public TimeUtil.Unit getUnit() {
        return TimeUtil.Unit.MINUTES;
    }

    @Override
    public ContentPathKeys newTime(DateTime pathTime, Collection<ContentKey> keys) {
        return new MinutePath(pathTime, keys);
    }

    @Override
    public ContentPath getNone() {
        return MinutePath.NONE;
    }
}
