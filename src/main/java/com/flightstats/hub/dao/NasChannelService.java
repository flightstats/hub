package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;

import java.util.Collection;

public class NasChannelService implements ChannelService {
    @Override
    public boolean channelExists(String channelName) {
        return false;
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        return null;
    }

    @Override
    public ContentKey insert(String channelName, Content content) {
        return null;
    }

    @Override
    public Optional<Content> getValue(Request request) {
        return null;
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        return null;
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return null;
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels(String tag) {
        return null;
    }

    @Override
    public Iterable<String> getTags() {
        return null;
    }

    @Override
    public ChannelConfiguration updateChannel(ChannelConfiguration configuration) {
        return null;
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery timeQuery) {
        return null;
    }

    @Override
    public Collection<ContentKey> getKeys(DirectionQuery query) {
        return null;
    }

    @Override
    public boolean delete(String channelName) {
        return false;
    }

    @Override
    public boolean isReplicating(String channelName) {
        return false;
    }

    @Override
    public Optional<ContentKey> getLatest(String channelName, boolean stable, boolean trace) {
        return null;
    }
}
