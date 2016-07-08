package com.flightstats.hub.webhook;

import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.Collection;

class MinuteTimedWebhook implements TimedWebhook {

    static final MinuteTimedWebhook WEBHOOK = new MinuteTimedWebhook();

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

    @Override
    public DateTime getReplicatingStable(ContentPath contentPath) {
        if (contentPath instanceof SecondPath) {
            SecondPath secondPath = (SecondPath) contentPath;
            if (secondPath.getTime().getSecondOfMinute() < 59) {
                return getUnit().round(contentPath.getTime().minusMinutes(1));
            }
        } else if (contentPath instanceof ContentKey) {
            return getUnit().round(contentPath.getTime().minusMinutes(1));
        }
        return getUnit().round(contentPath.getTime());
    }
}
