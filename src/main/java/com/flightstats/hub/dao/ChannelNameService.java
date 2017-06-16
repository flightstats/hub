package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;

/**
 * ChannelNameService will normalize the names of the channel.
 */
public class ChannelNameService implements ChannelService {

    @Inject
    @Named(ChannelService.DELEGATE)
    private ChannelService delegate;

    @Override
    public boolean channelExists(String channelName) {
        return delegate.channelExists(channelName);
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig configuration) {
        return delegate.createChannel(configuration);
    }

    @Override
    public ChannelConfig updateChannel(ChannelConfig configuration, ChannelConfig oldConfig, boolean isLocalHost) {
        return delegate.updateChannel(configuration, oldConfig, isLocalHost);
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        return delegate.insert(getDisplayName(channelName), content);
    }

    private String getDisplayName(String channelName) {
        return getCachedChannelConfig(channelName).getDisplayName();
    }

    @Override
    public boolean historicalInsert(String channelName, Content content) throws Exception {
        return delegate.historicalInsert(getDisplayName(channelName), content);
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        return delegate.insert(bulkContent.withChannel(getDisplayName(bulkContent.getChannel())));
    }

    @Override
    public boolean isReplicating(String channelName) {
        return delegate.isReplicating(channelName);
    }

    @Override
    public Optional<ContentKey> getLatest(DirectionQuery query) {
        return delegate.getLatest(query.withChannelName(getDisplayName(query.getChannelName())));
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, boolean stable) {
        return delegate.getLatest(getDisplayName(channel), stable);
    }

    @Override
    public void deleteBefore(String channel, ContentKey limitKey) {
        delegate.deleteBefore(getDisplayName(channel), limitKey);
    }

    @Override
    public Optional<Content> get(ItemRequest itemRequest) {
        return delegate.get(itemRequest.withChannel(getDisplayName(itemRequest.getChannel())));
    }

    @Override
    public void get(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        delegate.get(getDisplayName(channel), keys, callback);
    }

    @Override
    public ChannelConfig getChannelConfig(String channelName, boolean allowChannelCache) {
        return delegate.getChannelConfig(channelName, allowChannelCache);
    }

    @Override
    public ChannelConfig getCachedChannelConfig(String channelName) {
        return delegate.getCachedChannelConfig(channelName);
    }

    //todo - gfm - check for usages of this
    @Override
    public Collection<ChannelConfig> getChannels() {
        return delegate.getChannels();
    }

    //todo - gfm - check for usages of this
    @Override
    public Collection<ChannelConfig> getChannels(String tag, boolean useCache) {
        return delegate.getChannels(tag, useCache);
    }

    @Override
    public Iterable<String> getTags() {
        return delegate.getTags();
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        return delegate.queryByTime(query.withChannelName(getDisplayName(query.getChannelName())));
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        return delegate.query(query.withChannelName(getDisplayName(query.getChannelName())));
    }

    @Override
    public boolean delete(String channelName) {
        return delegate.delete(getDisplayName(channelName));
    }

    @Override
    public boolean delete(String channelName, ContentKey contentKey) {
        return delegate.delete(getDisplayName(channelName), contentKey);
    }

    @Override
    public ContentPath getLastUpdated(String channelName, ContentPath defaultValue) {
        return delegate.getLastUpdated(getDisplayName(channelName), defaultValue);
    }
}
