package com.flightstats.hub.group;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.ContentPathKeys;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.Collection;

public class SecondTimedGroup implements TimedGroup {

    public static final SecondTimedGroup GROUP = new SecondTimedGroup();

    @Override
    public int getOffsetSeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 1;
    }

    @Override
    public TimeUtil.Unit getUnit() {
        return TimeUtil.Unit.SECONDS;
    }

    @Override
    public ContentPathKeys newTime(DateTime pathTime, Collection<ContentKey> keys) {
        return new SecondPath(pathTime, keys);
    }

    @Override
    public ContentPath getNone() {
        return SecondPath.NONE;
    }
}
