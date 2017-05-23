package com.flightstats.hub.cluster;

import com.flightstats.hub.util.Hash;

import java.util.*;

//todo - gfm - this only exists to let some tests continue to pass without rework.
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

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof EqualRangesStrategy)) return false;
        final EqualRangesStrategy other = (EqualRangesStrategy) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$spokeNodes = this.spokeNodes;
        final Object other$spokeNodes = other.spokeNodes;
        if (this$spokeNodes == null ? other$spokeNodes != null : !this$spokeNodes.equals(other$spokeNodes))
            return false;
        if (this.rangeSize != other.rangeSize) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $spokeNodes = this.spokeNodes;
        result = result * PRIME + ($spokeNodes == null ? 43 : $spokeNodes.hashCode());
        final long $rangeSize = this.rangeSize;
        result = result * PRIME + (int) ($rangeSize >>> 32 ^ $rangeSize);
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof EqualRangesStrategy;
    }
}
