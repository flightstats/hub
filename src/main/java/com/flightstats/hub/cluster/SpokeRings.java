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
import java.util.concurrent.TimeUnit;

public class SpokeRings implements Ring, Comparable<SpokeRings> {

    private static final Logger logger = LoggerFactory.getLogger(SpokeRings.class);

    private LinkedList<SpokeRing> spokeRings = new LinkedList<>();

    /**
     * Processes a Collection of ClusterEvents into a List of Rings.
     */
    public void process(Collection<ClusterEvent> events) {
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
        DateTime now = TimeUtil.now();
        DateTime spokeTtl = now.minusMinutes(getSpokeTtlMinutes());
        logger.info("spoke minutes {} spokeTTL {} millis {}", HubProperties.getSpokeTtlMinutes(), spokeTtl, spokeTtl.getMillis());
        for (SpokeRing ring : initialRings) {
            if (ring.overlaps(spokeTtl, now)) {
                newRings.add(ring);
                logger.debug("new ring {}", ring);
            } else {
                logger.debug("old ring {}", ring);
            }
        }
        spokeRings = newRings;
    }

    private int getSpokeTtlMinutes() {
        return HubProperties.getSpokeTtlMinutes() + 1;
    }

    /**
     * Returns the ClusterEvents which are out of date and can be deleted.
     */
    Collection<ClusterEvent> generateOld(Collection<ClusterEvent> events) {
        long spokeTtlTime = System.currentTimeMillis() - TimeUnit.MILLISECONDS.convert(getSpokeTtlMinutes(), TimeUnit.MINUTES);
        Set<ClusterEvent> oldEvents = ClusterEvent.set();
        for (ClusterEvent event : events) {
            if (!event.isAdded() && event.getModifiedTime() < spokeTtlTime) {
                for (ClusterEvent clusterEvent : events) {
                    if (clusterEvent.getCreationTime() == event.getCreationTime()) {
                        oldEvents.add(clusterEvent);
                    }
                }
            }
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


    @Override
    public int compareTo(SpokeRings other) {
        int diff = spokeRings.size() - other.spokeRings.size();
        if (diff == 0) {
            for (int i = 0; i < spokeRings.size(); i++) {
                SpokeRing ring1 = spokeRings.get(i);
                SpokeRing ring2 = other.spokeRings.get(i);
                if (!ring1.equals(ring2)) {
                    logger.info("unequal ring1 {} ", ring1);
                    logger.info("unequal ring2 {} ", ring2);
                    return -1;
                }
            }
        }
        return diff;
    }
}
