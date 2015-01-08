package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ReplicationValidator;
import com.flightstats.hub.replication.V1ChannelReplicator;
import com.flightstats.hub.service.ChannelValidator;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;

import java.util.*;

public class ChannelServiceImpl implements ChannelService {

    private final ContentService contentService;
    private final ChannelConfigurationDao channelConfigurationDao;
    private final ChannelValidator channelValidator;
    private final V1ChannelReplicator v1ChannelReplicator;
    private final ReplicationValidator replicationValidator;
    private HostedGraphiteSender sender;

    @Inject
    public ChannelServiceImpl(ContentService contentService, ChannelConfigurationDao channelConfigurationDao,
                              ChannelValidator channelValidator, V1ChannelReplicator v1ChannelReplicator,
                              ReplicationValidator replicationValidator, HostedGraphiteSender sender) {
        this.contentService = contentService;
        this.channelConfigurationDao = channelConfigurationDao;
        this.channelValidator = channelValidator;
        this.v1ChannelReplicator = v1ChannelReplicator;
        this.replicationValidator = replicationValidator;
        this.sender = sender;
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
        if (content.isNew()) {
            replicationValidator.throwExceptionIfReplicating(channelName);
        }
        long start = System.currentTimeMillis();
        ContentKey contentKey = contentService.insert(channelName, content);
        sender.send("channel." + channelName + ".post", System.currentTimeMillis() - start);
        return contentKey;
    }

    public boolean isReplicating(String channelName) {
        return replicationValidator.isReplicating(channelName);
    }

    @Override
    public Optional<ContentKey> getLatest(String channelName, boolean stable) {
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channelName)
                .contentKey(new ContentKey(TimeUtil.time(stable), "ZZZZZ"))
                .next(false)
                .stable(stable)
                .count(1).build();
        query.trace(false);
        Collection<ContentKey> keys = getKeys(query);
        if (keys.isEmpty()) {
            return Optional.absent();
        } else {
            return Optional.of(keys.iterator().next());
        }
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
        Set<ContentKey> toReturn = new TreeSet<>();
        if (query.getCount() <= 0) {
            query.getTraces().add("requested zero");
            return toReturn;
        }
        List<ContentKey> keys = new ArrayList<>(contentService.getKeys(query));
        if (query.isNext()) {
            for (ContentKey key : keys) {
                toReturn.add(key);
                if (toReturn.size() >= query.getCount()) {
                    return toReturn;
                }
            }
        } else {
            for (int i = keys.size() - 1; i >= 0; i--) {
                toReturn.add(keys.get(i));
                if (toReturn.size() >= query.getCount()) {
                    return toReturn;
                }
            }
        }
        return toReturn;
    }

    @Override
    public boolean delete(String channelName) {
        if (!channelConfigurationDao.channelExists(channelName)) {
            return false;
        }
        replicationValidator.throwExceptionIfReplicating(channelName);
        contentService.delete(channelName);
        channelConfigurationDao.delete(channelName);
        v1ChannelReplicator.delete(channelName);
        return true;
    }
}
