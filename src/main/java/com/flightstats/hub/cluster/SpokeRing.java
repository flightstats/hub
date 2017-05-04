package com.flightstats.hub.cluster;

import com.flightstats.hub.util.Hash;
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

    private List<String> spokeNodes;
    private long rangeSize;
    private TimeInterval timeInterval;
    private DateTime startTime;

    //todo - gfm - remove these test only ctors
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
        //todo - gfm - maybe the times should overlap a bit ...
        previousRing.setEndTime(this.startTime);
        HashSet<String> nodes = new HashSet<>(previousRing.spokeNodes);
        if (clusterEvent.isAdded()) {
            nodes.add(clusterEvent.getName());
        } else {
            nodes.remove(clusterEvent.getName());
        }
        initialize(nodes);
    }

    private void setStartTime(ClusterEvent clusterEvent) {
        this.startTime = new DateTime(clusterEvent.getTime(), DateTimeZone.UTC);
        timeInterval = new TimeInterval(startTime, null);
    }

    private void initialize(Collection<String> nodes) {
        Map<Long, String> hashedNodes = new TreeMap<>();
        for (String node : nodes) {
            hashedNodes.put(Hash.hash(node), node);
        }
        rangeSize = Hash.getRangeSize(hashedNodes.size());
        spokeNodes = new ArrayList<>(hashedNodes.values());
    }

    void setEndTime(DateTime endTime) {
        timeInterval = new TimeInterval(startTime, endTime);
    }

    public Collection<String> getNodes(String channel) {
        if (spokeNodes.size() <= 3) {
            return spokeNodes;
        }
        long hash = Hash.hash(channel);
        int node = (int) (hash / rangeSize);
        if (hash < 0) {
            node = spokeNodes.size() + node - 1;
        }
        Collection<String> found = new ArrayList<>();
        int minimum = Math.min(3, spokeNodes.size());
        while (found.size() < minimum) {
            if (node == spokeNodes.size()) {
                node = 0;
            }
            found.add(spokeNodes.get(node));
            node++;

        }
        return found;
    }

    public Collection<String> getNodes(String channel, DateTime pointInTime) {
        if (timeInterval.contains(pointInTime)) {
            return getNodes(channel);
        }
        return Collections.emptyList();
    }

    public Collection<String> getNodes(String channel, DateTime startTime, DateTime endTime) {
        if (timeInterval.overlaps(startTime, endTime)) {
            return getNodes(channel);
        }
        return Collections.emptyList();
    }

    boolean endsBefore(DateTime endTime) {
        return !timeInterval.isAfter(endTime);
    }

    @Override
    public String toString() {
        return "SpokeRing{" +
                "startTime=" + startTime +
                ", spokeNodes=" + spokeNodes +
                ", timeInterval=" + timeInterval +
                ", rangeSize=" + rangeSize +
                '}';
    }
}
