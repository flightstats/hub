package com.flightstats.hub.dao;

import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.metrics.HostedGraphiteSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.Replicator;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class ChannelServiceImpl implements ChannelService {
    private final static Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final ContentService contentService;
    private final ChannelConfigurationDao channelConfigurationDao;
    private final ChannelValidator channelValidator;
    private final Replicator replicator;
    private HostedGraphiteSender sender;

    @Inject
    public ChannelServiceImpl(ContentService contentService, ChannelConfigurationDao channelConfigurationDao,
                              ChannelValidator channelValidator, Replicator replicator,
                              HostedGraphiteSender sender) {
        this.contentService = contentService;
        this.channelConfigurationDao = channelConfigurationDao;
        this.channelValidator = channelValidator;
        this.replicator = replicator;
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
        ChannelConfiguration created = channelConfigurationDao.createChannel(configuration);
        if (created.isReplicating()) {
            replicator.notifyWatchers();
        }
        return created;
    }

    @Override
    public ContentKey insert(String channelName, Content content) {
        //todo - gfm - 1/21/15 - this check may go away when we support inserting content into a replicating channel.
        if (content.isNew()) {
            throwExceptionIfReplicating(channelName);
        }
        long start = System.currentTimeMillis();
        ContentKey contentKey = contentService.insert(channelName, content);
        long time = System.currentTimeMillis() - start;
        sender.send("channel." + channelName + ".post", time);
        sender.send("channel.ALL.post", time);
        return contentKey;
    }

    private void throwExceptionIfReplicating(String channelName) {
        if (isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
    }

    public boolean isReplicating(String channelName) {
        ChannelConfiguration configuration = getChannelConfiguration(channelName);
        if (null == configuration) {
            return false;
        }
        return configuration.isReplicating();
    }

    @Override
    public Optional<ContentKey> getLatest(String channelName, boolean stable, boolean trace) {
        ChannelConfiguration channelConfiguration = getChannelConfiguration(channelName);
        if (null == channelConfiguration) {
            return Optional.absent();
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channelName)
                .contentKey(new ContentKey(TimeUtil.time(stable), "ZZZZZ"))
                .next(false)
                .stable(stable)
                .ttlDays(channelConfiguration.getTtlDays())
                .count(1).build();
        query.trace(trace);
        Collection<ContentKey> keys = getKeys(query);
        if (trace) {
            query.getTraces().log(logger);
        }
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
        ChannelConfiguration oldConfig = getChannelConfiguration(configuration.getName());
        channelValidator.validate(configuration, false);
        channelConfigurationDao.updateChannel(configuration);
        if (oldConfig.isReplicating() || configuration.isReplicating()) {
            replicator.notifyWatchers();
        }
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
        boolean replicating = isReplicating(channelName);
        contentService.delete(channelName);
        channelConfigurationDao.delete(channelName);
        if (replicating) {
            replicator.notifyWatchers();
        }
        return true;
    }
}
