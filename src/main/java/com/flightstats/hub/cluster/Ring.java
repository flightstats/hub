package com.flightstats.hub.cluster;

import org.joda.time.DateTime;

import java.util.Collection;

public interface Ring {
    Collection<String> getNodes(String channel);

    Collection<String> getNodes(String channel, DateTime pointInTime);

    Collection<String> getNodes(String channel, DateTime startTime, DateTime endTime);
}
