package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ChannelReplicator;
import com.flightstats.hub.replication.ReplicationValidator;
import com.flightstats.hub.service.ChannelValidator;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

public class ChannelServiceImpl implements ChannelService {

    private final ContentService contentService;
    private final ChannelConfigurationDao channelConfigurationDao;
    private final ChannelValidator channelValidator;
    private final ChannelReplicator channelReplicator;
    private final ReplicationValidator replicationValidator;

    @Inject
    public ChannelServiceImpl(ContentService contentService, ChannelConfigurationDao channelConfigurationDao,
                              ChannelValidator channelValidator,
                              ChannelReplicator channelReplicator, ReplicationValidator replicationValidator) {
        this.contentService = contentService;
        this.channelConfigurationDao = channelConfigurationDao;
        this.channelValidator = channelValidator;
        this.channelReplicator = channelReplicator;
        this.replicationValidator = replicationValidator;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelConfigurationDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        channelValidator.validate(configuration, true);
        configuration = ChannelConfiguration.builder().withChannelConfiguration(configuration).build();
        return channelConfigurationDao.createChannel(configuration);
    }

    @Override
    public ContentKey insert(String channelName, Content content) {
        if (content.isNewContent()) {
            replicationValidator.throwExceptionIfReplicating(channelName);
        }
        return contentService.insert(channelName, content);
    }

    @Override
    public Optional<Content> getValue(Request request) {
        return contentService.getValue(request.getChannel(), request.getKey());
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        return channelConfigurationDao.getChannelConfiguration(channelName);
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return channelConfigurationDao.getChannels();
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels(String tag) {
        Collection<ChannelConfiguration> matchingChannels = new ArrayList<>();
        Iterable<ChannelConfiguration> channels = getChannels();
        for (ChannelConfiguration channel : channels) {
            if (channel.getTags().contains(tag)) {
                matchingChannels.add(channel);
            }
        }
        return matchingChannels;
    }

    @Override
    public Iterable<String> getTags() {
        Collection<String> matchingChannels = new HashSet<>();
        Iterable<ChannelConfiguration> channels = getChannels();
        for (ChannelConfiguration channel : channels) {
            matchingChannels.addAll(channel.getTags());
        }
        return matchingChannels;
    }

    @Override
    public Optional<ContentKey> findLastUpdatedKey(String channelName) {
        return contentService.findLastUpdatedKey(channelName);
    }

    @Override
    public ChannelConfiguration updateChannel(ChannelConfiguration configuration) {
        configuration = ChannelConfiguration.builder().withChannelConfiguration(configuration).build();
        channelValidator.validate(configuration, false);
        channelConfigurationDao.updateChannel(configuration);
        return configuration;
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery timeQuery) {
        DateTime stableTime = TimeUtil.stable();
        Collection<ContentKey> contentKeys = contentService.queryByTime(timeQuery);
        if (timeQuery.isStable()) {
            ArrayList<ContentKey> remove = new ArrayList<>();
            for (ContentKey contentKey : contentKeys) {
                if (contentKey.getTime().isAfter(stableTime)) {
                    remove.add(contentKey);
                }
            }
            contentKeys.removeAll(remove);
        }
        return contentKeys;
    }

    @Override
    public Collection<ContentKey> getKeys(DirectionQuery query) {
        return contentService.getKeys(query);
    }

    @Override
    public boolean delete(String channelName) {
        if (!channelConfigurationDao.channelExists(channelName)) {
            return false;
        }
        replicationValidator.throwExceptionIfReplicating(channelName);
        contentService.delete(channelName);
        channelConfigurationDao.delete(channelName);
        channelReplicator.delete(channelName);
        return true;
    }
}
