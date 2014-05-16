package com.flightstats.hub.dao;

import com.flightstats.hub.dao.timeIndex.TimeIndexProcessor;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ChannelReplicator;
import com.flightstats.hub.replication.ReplicationValidator;
import com.flightstats.hub.service.CreateChannelValidator;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

/**
 *
 */
public class ChannelServiceImpl implements ChannelService {

    private final static Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final ContentService contentService;
    private final ChannelConfigurationDao channelConfigurationDao;
    private final CreateChannelValidator createChannelValidator;
    private final TimeIndexProcessor timeIndexProcessor;
    private final ChannelReplicator channelReplicator;
    private final ReplicationValidator replicationValidator;

    @Inject
    public ChannelServiceImpl(ContentService contentService, ChannelConfigurationDao channelConfigurationDao,
                              CreateChannelValidator createChannelValidator, TimeIndexProcessor timeIndexProcessor,
                              ChannelReplicator channelReplicator, ReplicationValidator replicationValidator) {
        this.contentService = contentService;
        this.channelConfigurationDao = channelConfigurationDao;
        this.createChannelValidator = createChannelValidator;
        this.timeIndexProcessor = timeIndexProcessor;
        this.channelReplicator = channelReplicator;
        this.replicationValidator = replicationValidator;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelConfigurationDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        createChannelValidator.validate(configuration);
        configuration = ChannelConfiguration.builder().withChannelConfiguration(configuration).build();
        contentService.createChannel(configuration);
        return channelConfigurationDao.createChannel(configuration);
    }

    @Override
    public InsertedContentKey insert(String channelName, Content content) {
        if (content.isNewContent()) {
            replicationValidator.preventInsertIfReplicating(channelName);
        }
        ChannelConfiguration configuration = channelConfigurationDao.getChannelConfiguration(channelName);
        return contentService.insert(configuration, content);
    }

    @Override
    public Optional<LinkedContent> getValue(Request request) {
        return contentService.getValue(request.getChannel(), request.getId());
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
    public boolean isHealthy() {
        return channelConfigurationDao.isHealthy();
    }

    @Override
    public ChannelConfiguration updateChannel(ChannelConfiguration configuration) {
        configuration = ChannelConfiguration.builder().withChannelConfiguration(configuration).build();
        contentService.updateChannel(configuration);
        channelConfigurationDao.updateChannel(configuration);
        return configuration;
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        return contentService.getKeys(channelName, dateTime);
    }

    @Override
    public boolean delete(String channelName) {
        if (!channelConfigurationDao.channelExists(channelName)) {
            return false;
        }
        contentService.delete(channelName);
        channelConfigurationDao.delete(channelName);
        timeIndexProcessor.delete(channelName);
        channelReplicator.delete(channelName);
        return true;
    }
}
