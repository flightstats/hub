package com.flightstats.hub.cluster;

import com.flightstats.hub.util.Hash;
import com.flightstats.hub.util.TimeInterval;
import org.joda.time.DateTime;

import java.util.*;

/**
 * A SpokeRing represents the state of a cluster over a period of time.
 * There must always be a start time.
 * The end time is only used if this cluster is no longer active.
 */
class SpokeRing implements Ring {

    private final List<SpokeNode> spokeNodes;
    private final long rangeSize;
    private TimeInterval timeInterval;

    SpokeRing(DateTime startTime, SpokeNode... nodes) {
        this(startTime, null, nodes);
    }

    SpokeRing(DateTime startTime, DateTime endTime, SpokeNode... nodes) {
        timeInterval = new TimeInterval(startTime, endTime);
        Map<Long, SpokeNode> hashedNodes = new TreeMap<>();
        for (SpokeNode node : nodes) {
            hashedNodes.put(Hash.hash(node.getName()), node);
        }
        rangeSize = Hash.getRangeSize(hashedNodes.size());
        spokeNodes = new ArrayList<>(hashedNodes.values());
    }

    public Collection<SpokeNode> getNodes(String channel) {
        long hash = Hash.hash(channel);
        int node = (int) (hash / rangeSize);
        if (hash < 0) {
            node = spokeNodes.size() + node - 1;
        }
        Collection<SpokeNode> found = new ArrayList<>();
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

    public Collection<SpokeNode> getNodes(String channel, DateTime pointInTime) {
        if (timeInterval.contains(pointInTime)) {
            return getNodes(channel);
        }
        return Collections.emptyList();
    }

    public Collection<SpokeNode> getNodes(String channel, DateTime startTime, DateTime endTime) {
        if (timeInterval.overlaps(startTime, endTime)) {
            return getNodes(channel);
        }
        return Collections.emptyList();
    }

}
