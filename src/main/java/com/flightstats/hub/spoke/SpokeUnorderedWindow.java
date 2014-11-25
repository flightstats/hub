package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicLong;

public class SpokeUnorderedWindow {

    private final static Logger logger = LoggerFactory.getLogger(SpokeUnorderedWindow.class);

    private static final AtomicLong maxDifference = new AtomicLong(-1);

    public static void report(ContentKey key, long complete) {
        long difference = complete - key.getMillis();
        long currentMax = maxDifference.get();
        if (difference > currentMax) {
            logger.info("new current max unordered window " + difference);
            maxDifference.compareAndSet(currentMax, difference);
        }
    }
}
