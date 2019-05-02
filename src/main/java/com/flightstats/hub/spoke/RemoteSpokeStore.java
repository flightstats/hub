package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;

public interface RemoteSpokeStore {
    boolean insert(SpokeStore spokeStore, String path, byte[] payload, String spokeApi, String channel);
    boolean insert(SpokeStore spokeStore, String path, byte[] payload, Collection<String> servers, Traces traces,
                   String spokeApi, String channel);
    Content get(SpokeStore spokeStore, String path, ContentKey key);
    Optional<ContentKey> getLatest(String channel, String path, Traces traces) throws InterruptedException;
    boolean delete(SpokeStore spokeStore, String path) throws Exception;

    void testOne(Collection<String> server) throws InterruptedException;
    boolean testAll() throws UnknownHostException;

    QueryResult readTimeBucket(SpokeStore spokeStore, String channel, String timePath) throws InterruptedException;
    SortedSet<ContentKey> getNext(String channel, int count, String startKey) throws InterruptedException;
}
