package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.Hash;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import lombok.EqualsAndHashCode;

import java.util.*;

@EqualsAndHashCode
public class ConsistentHashStrategy implements RingStrategy {

    private final HashFunction hashFunction = Hashing.farmHashFingerprint64();
    private final int numberOfReplicas = HubProperties.getProperty("consistent.hashing.replicas", 256);
    private final SortedMap<Long, String> circle = new TreeMap<>();
    private List<String> spokeNodes = new ArrayList<>();

    ConsistentHashStrategy(Collection<String> nodes) {
        for (String node : nodes) {
            for (int i = 0; i < numberOfReplicas; i++) {
                circle.put(Hash.hash(node + i), node);
            }
        }
        spokeNodes = new ArrayList<>(nodes);
    }

    @Override
    public Set<String> getServers(String channel) {
        if (spokeNodes.size() <= 3) {
            return new HashSet<>(spokeNodes);
        }

        long hash = Hash.hash(channel);
        SortedMap<Long, String> startPoint = circle.tailMap(hash);
        if (startPoint.isEmpty()) {
            startPoint = circle;
        }
        Set<String> nodes = new HashSet<>();
        for (String node : startPoint.values()) {
            nodes.add(node);
            if (nodes.size() >= 3) {
                return nodes;
            }
        }
        for (String node : circle.values()) {
            nodes.add(node);
            if (nodes.size() >= 3) {
                return nodes;
            }
        }
        return nodes;
    }

    @Override
    public List<String> getAllServers() {
        return spokeNodes;
    }


}
