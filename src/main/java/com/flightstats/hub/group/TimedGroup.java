package com.flightstats.hub.group;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.ContentPathKeys;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.Collection;

public interface TimedGroup {

    int getOffsetSeconds();

    int getPeriodSeconds();

    TimeUtil.Unit getUnit();

    ContentPathKeys newTime(DateTime pathTime, Collection<ContentKey> keys);

    ContentPath getNone();

    static TimedGroup getTimedGroup(Group group) {
        if (group.isSecond()) {
            return SecondTimedGroup.GROUP;
        }
        if (group.isMinute()) {
            return MinuteTimedGroup.GROUP;
        }

        throw new UnsupportedOperationException("invalid incoming group " + group);
    }

    DateTime getReplicatingStable(ContentPath contentPath);
}
