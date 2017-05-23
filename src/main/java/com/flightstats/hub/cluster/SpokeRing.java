package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.metrics.ActiveTraces;
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
    private ClusterEvent clusterEvent;
    private RingStrategy strategy;

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
        this.clusterEvent = clusterEvent;
        timeInterval = new TimeInterval(startTime, null);
    }

    private void initialize(Collection<String> nodes) {
        String property = HubProperties.getProperty("spoke.ring.strategy", "ConsistentHashStrategy");
        if (property.equalsIgnoreCase("EqualRangesStrategy")) {
            strategy = new EqualRangesStrategy(nodes);
        } else {
            strategy = new ConsistentHashStrategy(nodes);
        }
    }

    void setEndTime(DateTime endTime) {
        timeInterval = new TimeInterval(startTime, endTime);
    }

    public Set<String> getServers(String channel) {
        Set<String> servers = strategy.getServers(channel);
        ActiveTraces.getLocal().add("ring servers ", servers);
        return servers;
    }

    public Set<String> getServers(String channel, DateTime pointInTime) {
        if (timeInterval.contains(pointInTime)) {
            ActiveTraces.getLocal().add("ring interval {} contains start {}", this.startTime, startTime);
            return getServers(channel);
        }
        return Collections.emptySet();
    }

    public Set<String> getServers(String channel, DateTime startTime, DateTime endTime) {
        if (overlaps(startTime, endTime)) {
            ActiveTraces.getLocal().add("ring interval {} overlaps start {} end {}", this.startTime, startTime, endTime);
            return getServers(channel);
        }
        return Collections.emptySet();
    }

    boolean overlaps(DateTime startTime, DateTime endTime) {
        return timeInterval.overlaps(startTime, endTime);
    }

    boolean endsBefore(DateTime endTime) {
        return !timeInterval.isAfter(endTime);
    }

    @Override
    public String toString() {
        return "SpokeRing{" +
                "nodes=" + strategy.getAllServers() +
                ", timeInterval=" + timeInterval +
                ", clusterEvent=" + clusterEvent +
                '}';
    }

    ClusterEvent getClusterEvent() {
        return clusterEvent;
    }

    public void status(ObjectNode root) {
        root.put("nodes", strategy.getAllServers().toString());
        timeInterval.status(root);
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof SpokeRing)) return false;
        final SpokeRing other = (SpokeRing) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$clusterEvent = this.getClusterEvent();
        final Object other$clusterEvent = other.getClusterEvent();
        if (this$clusterEvent == null ? other$clusterEvent != null : !this$clusterEvent.equals(other$clusterEvent))
            return false;
        final Object this$strategy = this.strategy;
        final Object other$strategy = other.strategy;
        if (this$strategy == null ? other$strategy != null : !this$strategy.equals(other$strategy)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $clusterEvent = this.getClusterEvent();
        result = result * PRIME + ($clusterEvent == null ? 43 : $clusterEvent.hashCode());
        final Object $strategy = this.strategy;
        result = result * PRIME + ($strategy == null ? 43 : $strategy.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof SpokeRing;
    }
}
