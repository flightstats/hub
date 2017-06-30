package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.Hash;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;

import java.util.*;

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


    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ConsistentHashStrategy)) return false;
        final ConsistentHashStrategy other = (ConsistentHashStrategy) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$hashFunction = this.hashFunction;
        final Object other$hashFunction = other.hashFunction;
        if (this$hashFunction == null ? other$hashFunction != null : !this$hashFunction.equals(other$hashFunction))
            return false;
        if (this.numberOfReplicas != other.numberOfReplicas) return false;
        final Object this$circle = this.circle;
        final Object other$circle = other.circle;
        if (this$circle == null ? other$circle != null : !this$circle.equals(other$circle)) return false;
        final Object this$spokeNodes = this.spokeNodes;
        final Object other$spokeNodes = other.spokeNodes;
        if (this$spokeNodes == null ? other$spokeNodes != null : !this$spokeNodes.equals(other$spokeNodes))
            return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $hashFunction = this.hashFunction;
        result = result * PRIME + ($hashFunction == null ? 43 : $hashFunction.hashCode());
        result = result * PRIME + this.numberOfReplicas;
        final Object $circle = this.circle;
        result = result * PRIME + ($circle == null ? 43 : $circle.hashCode());
        final Object $spokeNodes = this.spokeNodes;
        result = result * PRIME + ($spokeNodes == null ? 43 : $spokeNodes.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ConsistentHashStrategy;
    }
}
