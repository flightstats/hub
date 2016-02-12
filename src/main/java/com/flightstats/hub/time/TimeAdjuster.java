package com.flightstats.hub.time;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TimeAdjuster {
    private final static Logger logger = LoggerFactory.getLogger(TimeAdjuster.class);

    private volatile int offset;
    private volatile long lowerBarMillis;

    public DateTime getAdjustedNow() {
        return getAdjustedNow(System.currentTimeMillis());
    }

    @VisibleForTesting
    DateTime getAdjustedNow(long currentTimeMillis) {
        long millis = currentTimeMillis + offset;
        return new DateTime(Math.max(millis, lowerBarMillis), DateTimeZone.UTC);
    }

    /**
     * @param serverNtpOffset should be the number directly reported by ntp for the servers.
     */
    public void setOffset(int serverNtpOffset) {
        setOffset(serverNtpOffset, System.currentTimeMillis());
    }

    @VisibleForTesting
    void setOffset(int serverNtpOffset, long millis) {
        int newOffset = -serverNtpOffset;
        if (newOffset == offset) {
            return;
        }
        if (newOffset < offset) {
            lowerBarMillis = millis + offset;
        }
        logger.info("setting new offset {} from {}", newOffset, offset);
        offset = newOffset;
    }
}
