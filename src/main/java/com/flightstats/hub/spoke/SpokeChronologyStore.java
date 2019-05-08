package com.flightstats.hub.spoke;

import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ContentKey;

import java.util.Optional;
import java.util.SortedSet;

public interface SpokeChronologyStore {
    SortedSet<ContentKey> getNextKeysFromCluster(String channel, int count, String startKey) throws InterruptedException;
    Optional<ContentKey> getLatestFromCluster(String channel, String path, Traces traces) throws InterruptedException;
}