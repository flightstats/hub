package com.flightstats.hub.webhook;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.ContentPathKeys;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.Collection;

interface TimedWebhook {

    int getOffsetSeconds();

    int getPeriodSeconds();

    TimeUtil.Unit getUnit();

    ContentPathKeys newTime(DateTime pathTime, Collection<ContentKey> keys);

    ContentPath getNone();

    static TimedWebhook getTimedWebhook(Webhook webhook) {
        if (webhook.isSecond()) {
            return SecondTimedWebhook.WEBHOOK;
        }
        if (webhook.isMinute()) {
            return MinuteTimedWebhook.WEBHOOK;
        }

        throw new UnsupportedOperationException("invalid incoming webhook " + webhook);
    }

    DateTime getReplicatingStable(ContentPath contentPath);
}
