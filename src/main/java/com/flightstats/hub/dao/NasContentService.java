package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;

public class NasContentService implements ContentService {
    @Override
    public ContentKey insert(String channelName, Content content) {
        return null;
    }

    @Override
    public Optional<Content> getValue(String channelName, ContentKey key) {
        return null;
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery timeQuery) {
        return null;
    }

    @Override
    public void delete(String channelName) {

    }

    @Override
    public Collection<ContentKey> getKeys(DirectionQuery query) {
        return null;
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        return null;
    }
}
