package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class SpokeRings implements Ring {

    private static final Logger logger = LoggerFactory.getLogger(SpokeRings.class);

    private LinkedList<SpokeRing> spokeRings = new LinkedList<>();

    /**
     * Processes a Collection of ClusterEvents into a List of Rings.
     * Returns the records which are out of date and can be deleted.
     */
    public List<String> process(Collection<ClusterEvent> events) {
        LinkedList<SpokeRing> initialRings = new LinkedList<>();
        for (ClusterEvent clusterEvent : events) {
            if (initialRings.isEmpty()) {
                if (clusterEvent.isAdded()) {
                    initialRings.add(new SpokeRing(clusterEvent));
                }
            } else {
                initialRings.add(new SpokeRing(clusterEvent, initialRings.getLast()));
            }
        }
        LinkedList<SpokeRing> newRings = new LinkedList<>();
        DateTime spokeTtl = TimeUtil.now().minusMinutes(HubProperties.getSpokeTtlMinutes());
        for (SpokeRing ring : initialRings) {
            if (!ring.endsBefore(spokeTtl)) {
                newRings.add(ring);
            }
        }
        if (logger.isDebugEnabled()) {
            for (SpokeRing newRing : newRings) {
                logger.debug("new ring {}", newRing);
            }
        }
        spokeRings = newRings;
        initialRings.removeAll(newRings);
        List<String> oldEvents = new ArrayList<>();
        for (SpokeRing ring : initialRings) {
            oldEvents.add(ring.getClusterEvent().event());
        }
        return oldEvents;
    }

    @Override
    public Set<String> getServers(String channel) {
        return spokeRings.getLast().getServers(channel);
    }

    @Override
    public Set<String> getServers(String channel, DateTime pointInTime) {
        Set<String> nodes = new HashSet<>();
        for (SpokeRing spokeRing : spokeRings) {
            nodes.addAll(spokeRing.getServers(channel, pointInTime));
        }
        return nodes;
    }

    @Override
    public Set<String> getServers(String channel, DateTime startTime, DateTime endTime) {
        Set<String> nodes = new HashSet<>();
        for (SpokeRing spokeRing : spokeRings) {
            nodes.addAll(spokeRing.getServers(channel, startTime, endTime));
        }
        return nodes;
    }

    public void status(ObjectNode root) {
        ArrayNode ringsNode = root.putArray("rings");
        for (SpokeRing ring : spokeRings) {
            ring.status(ringsNode.addObject());
        }
    }
}
