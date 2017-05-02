package com.flightstats.hub.cluster;

import org.joda.time.DateTime;

import java.util.Collection;

public interface Ring {
    Collection<SpokeNode> getNodes(String channel);

    Collection<SpokeNode> getNodes(String channel, DateTime pointInTime);

    Collection<SpokeNode> getNodes(String channel, DateTime startTime, DateTime endTime);
}
