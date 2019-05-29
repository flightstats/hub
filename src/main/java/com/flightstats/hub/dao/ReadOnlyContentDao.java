package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;

import java.util.Optional;
import java.util.SortedSet;

public class ReadOnlyContentDao implements ContentDao {
    private final ContentDao delegate;

    public ReadOnlyContentDao(ContentDao delegate) {
        this.delegate = delegate;
    }

    @Override
    public ContentKey insert(String channelName, Content content) {
        throw new UnsupportedOperationException("Unable to insert due to r/o DAO:  " + channelName);
    }

    @Override
    public Content get(String channelName, ContentKey key) {
        return delegate.get(channelName, key);
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery timQuery) {
        return delegate.queryByTime(timQuery);
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        return delegate.query(query);
    }

    @Override
    public void delete(String channelName) {
        throw new UnsupportedOperationException("Unable to delete due to r/o DAO:  " + channelName);
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        return delegate.getLatest(channel, limitKey, traces);
    }

    @Override
    public void deleteBefore(String channelName, ContentKey limitKey) {
        throw new UnsupportedOperationException("Unable to deleteBefore due to r/o DAO:  " + channelName);
    }
}
