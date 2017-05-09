package com.flightstats.hub.cluster;

import com.flightstats.hub.util.Hash;

import java.util.*;

public class EqualRangesStrategy implements RingStrategy {

    private List<String> spokeNodes;
    private long rangeSize;

    EqualRangesStrategy(Collection<String> nodes) {
        if (nodes.isEmpty()) {
            spokeNodes = new ArrayList<>();
            return;
        }
        Map<Long, String> hashedNodes = new TreeMap<>();
        for (String node : nodes) {
            hashedNodes.put(Hash.hash(node), node);
        }
        rangeSize = Hash.getRangeSize(hashedNodes.size());
        spokeNodes = new ArrayList<>(hashedNodes.values());
    }

    @Override
    public Set<String> getServers(String channel) {
        if (spokeNodes.size() <= 3) {
            return new HashSet<>(spokeNodes);
        }
        long hash = Hash.hash(channel);
        int node = (int) (hash / rangeSize);
        if (hash < 0) {
            node = spokeNodes.size() + node - 1;
        }
        Set<String> found = new HashSet<>();
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

    @Override
    public List<String> getAllServers() {
        return spokeNodes;
    }

}
