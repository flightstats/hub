package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Optional;
import java.util.SortedSet;

public class ReadOnlyRemoteSpokeStore implements RemoteSpokeStore {
    private final RemoteSpokeStore delegate;

    @Inject
    public ReadOnlyRemoteSpokeStore(RemoteSpokeStore delegate) {
        this.delegate = delegate;
    }

    @Override
    public boolean insert(SpokeStore spokeStore, String path, byte[] payload, String spokeApi, String channel) {
        if (spokeStore == SpokeStore.WRITE) {
            throw new UnsupportedOperationException("Unable to insert to remote spoke store from a r/o node " + channel);
        } else {
            return delegate.insert(spokeStore, path, payload, spokeApi, channel);
        }
    }

    @Override
    public boolean insert(SpokeStore spokeStore, String path, byte[] payload, Collection<String> servers, Traces traces, String spokeApi, String channel) {
        if (spokeStore == SpokeStore.WRITE) {
            throw new UnsupportedOperationException("Unable to insert to remote spoke store on all servers from a r/o node " + channel);
        } else {
            return delegate.insert(spokeStore, path, payload, servers, traces, spokeApi, channel);
        }
    }

    @Override
    public Content get(SpokeStore spokeStore, String path, ContentKey key) {
        return delegate.get(spokeStore, path, key);
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, String path, Traces traces) throws InterruptedException {
        return delegate.getLatest(channel, path, traces);
    }

    @Override
    public boolean delete(SpokeStore spokeStore, String path) throws Exception {
        if (spokeStore == SpokeStore.WRITE) {
            throw new UnsupportedOperationException("Unable to delete from the remote spoke store from a r/o node " + path);
        } else {
            return delegate.delete(spokeStore, path);
        }
    }

    @Override
    public void testOne(Collection<String> servers) throws InterruptedException {
        delegate.testOne(servers);
    }

    @Override
    public boolean testAll() throws UnknownHostException {
        return delegate.testAll();
    }

    @Override
    public QueryResult readTimeBucket(SpokeStore spokeStore, String channel, String timePath) throws InterruptedException {
        return delegate.readTimeBucket(spokeStore, channel, timePath);
    }

    @Override
    public SortedSet<ContentKey> getNext(String channel, int count, String startKey) throws InterruptedException {
        return delegate.getNext(channel, count, startKey);
    }
}
