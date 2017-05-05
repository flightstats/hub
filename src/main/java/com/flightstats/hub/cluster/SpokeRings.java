package com.flightstats.hub.cluster;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class SpokeRings implements Ring {

    private static final Logger logger = LoggerFactory.getLogger(SpokeRings.class);

    private LinkedList<SpokeRing> spokeRings = new LinkedList<>();

    public void process(Collection<ClusterEvent> events) {
        LinkedList<SpokeRing> firstPass = new LinkedList<>();
        for (ClusterEvent clusterEvent : events) {
            if (firstPass.isEmpty()) {
                if (clusterEvent.isAdded()) {
                    firstPass.add(new SpokeRing(clusterEvent));
                }
            } else {
                firstPass.add(new SpokeRing(clusterEvent, firstPass.getLast()));
            }
        }
        LinkedList<SpokeRing> newRings = new LinkedList<>();
        DateTime spokeTtl = TimeUtil.now().minusMinutes(HubProperties.getSpokeTtl());
        for (SpokeRing ring : firstPass) {
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
    }

    @Override
    public Collection<String> getNodes(String channel) {
        return spokeRings.getLast().getNodes(channel);
    }

    @Override
    public Collection<String> getNodes(String channel, DateTime pointInTime) {
        Set<String> nodes = new HashSet<>();
        for (SpokeRing spokeRing : spokeRings) {
            nodes.addAll(spokeRing.getNodes(channel, pointInTime));
        }
        return nodes;
    }

    @Override
    public Collection<String> getNodes(String channel, DateTime startTime, DateTime endTime) {
        Set<String> nodes = new HashSet<>();
        for (SpokeRing spokeRing : spokeRings) {
            nodes.addAll(spokeRing.getNodes(channel, startTime, endTime));
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
