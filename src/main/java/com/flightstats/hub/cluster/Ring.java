package com.flightstats.hub.cluster;

import org.joda.time.DateTime;

import java.util.Set;

public interface Ring {
    Set<String> getServers(String channel);

    Set<String> getServers(String channel, DateTime pointInTime);

    Set<String> getServers(String channel, DateTime startTime, DateTime endTime);
}
