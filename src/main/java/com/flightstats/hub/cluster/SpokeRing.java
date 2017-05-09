package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.TimeInterval;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.*;

/**
 * A SpokeRing represents the state of a cluster over a period of time.
 * There must always be a start time.
 * The end time is only used if this cluster is no longer active.
 */
class SpokeRing implements Ring {

    private static final int OVERLAP_SECONDS = HubProperties.getProperty("spoke.ring.overlap.seconds", 1);

    private TimeInterval timeInterval;
    private DateTime startTime;
    private EqualRangesStrategy strategy;

    SpokeRing(DateTime startTime, String... nodes) {
        this(startTime, null, nodes);
    }

    SpokeRing(DateTime startTime, DateTime endTime, String... nodes) {
        this.startTime = startTime;
        timeInterval = new TimeInterval(startTime, endTime);
        initialize(Arrays.asList(nodes));
    }

    SpokeRing(ClusterEvent clusterEvent) {
        setStartTime(clusterEvent);
        initialize(Collections.singletonList(clusterEvent.getName()));
    }

    SpokeRing(ClusterEvent clusterEvent, SpokeRing previousRing) {
        setStartTime(clusterEvent);
        previousRing.setEndTime(
                new DateTime(clusterEvent.getModifiedTime(), DateTimeZone.UTC)
                        .plusSeconds(OVERLAP_SECONDS));
        HashSet<String> nodes = new HashSet<>(previousRing.strategy.getAllServers());
        if (clusterEvent.isAdded()) {
            nodes.add(clusterEvent.getName());
        } else {
            nodes.remove(clusterEvent.getName());
        }
        initialize(nodes);
    }

    private void setStartTime(ClusterEvent clusterEvent) {
        this.startTime = new DateTime(clusterEvent.getModifiedTime(), DateTimeZone.UTC);
        timeInterval = new TimeInterval(startTime, null);
    }

    private void initialize(Collection<String> nodes) {
        strategy = new EqualRangesStrategy(nodes);
    }

    void setEndTime(DateTime endTime) {
        timeInterval = new TimeInterval(startTime, endTime);
    }

    public Set<String> getServers(String channel) {
        return strategy.getServers(channel);
    }

    public Set<String> getServers(String channel, DateTime pointInTime) {
        if (timeInterval.contains(pointInTime)) {
            return getServers(channel);
        }
        return Collections.emptySet();
    }

    public Set<String> getServers(String channel, DateTime startTime, DateTime endTime) {
        if (timeInterval.overlaps(startTime, endTime)) {
            return getServers(channel);
        }
        return Collections.emptySet();
    }

    boolean endsBefore(DateTime endTime) {
        return !timeInterval.isAfter(endTime);
    }

    @Override
    public String toString() {
        return "SpokeRing{" +
                "startTime=" + startTime +
                ", timeInterval=" + timeInterval +
                '}';
    }

    public void status(ObjectNode root) {
        root.put("nodes", strategy.getAllServers().toString());
        timeInterval.status(root);
    }
}
