package com.flightstats.hub.dao;

import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.Replicator;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class ChannelServiceImpl implements ChannelService {
    private final static Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final ContentService contentService;
    private final ChannelConfigDao channelConfigDao;
    private final ChannelValidator channelValidator;
    private final Replicator replicator;
    private MetricsSender sender;

    @Inject
    public ChannelServiceImpl(ContentService contentService, ChannelConfigDao channelConfigDao,
                              ChannelValidator channelValidator, Replicator replicator,
                              MetricsSender sender) {
        this.contentService = contentService;
        this.channelConfigDao = channelConfigDao;
        this.channelValidator = channelValidator;
        this.replicator = replicator;
        this.sender = sender;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelConfigDao.channelExists(channelName);
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig configuration) {
        channelValidator.validate(configuration, true);
        configuration = ChannelConfig.builder().withChannelConfiguration(configuration).build();
        ChannelConfig created = channelConfigDao.createChannel(configuration);
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
        ChannelConfig configuration = getChannelConfiguration(channelName);
        if (null == configuration) {
            return false;
        }
        return configuration.isReplicating();
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, boolean stable, boolean trace) {
        ChannelConfig channelConfig = getChannelConfiguration(channel);
        if (null == channelConfig) {
            return Optional.absent();
        }
        Traces traces = Traces.NOOP;
        if (trace) {
            traces = new TracesImpl();
        }
        ContentKey limitKey = new ContentKey(TimeUtil.time(stable), "ZZZZZZ");
        Optional<ContentKey> latest = contentService.getLatest(channel, limitKey, traces);
        if (latest.isPresent()) {
            traces.log(logger);
            return latest;
        }

        //todo - gfm - 3/18/15 - fix this!
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(limitKey)
                .next(false)
                .stable(stable)
                .ttlDays(channelConfig.getTtlDays())
                .traces(traces)
                .count(1)
                .build();
        Collection<ContentKey> keys = getKeys(query);
        query.getTraces().log(logger);
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
    public ChannelConfig getChannelConfiguration(String channelName) {
        return channelConfigDao.getChannelConfig(channelName);
    }

    @Override
    public Iterable<ChannelConfig> getChannels() {
        return channelConfigDao.getChannels();
    }

    @Override
    public Iterable<ChannelConfig> getChannels(String tag) {
        Collection<ChannelConfig> matchingChannels = new ArrayList<>();
        Iterable<ChannelConfig> channels = getChannels();
        for (ChannelConfig channel : channels) {
            if (channel.getTags().contains(tag)) {
                matchingChannels.add(channel);
            }
        }
        return matchingChannels;
    }

    @Override
    public Iterable<String> getTags() {
        Collection<String> matchingChannels = new HashSet<>();
        Iterable<ChannelConfig> channels = getChannels();
        for (ChannelConfig channel : channels) {
            matchingChannels.addAll(channel.getTags());
        }
        return matchingChannels;
    }

    @Override
    public ChannelConfig updateChannel(ChannelConfig configuration) {
        configuration = ChannelConfig.builder().withChannelConfiguration(configuration).build();
        ChannelConfig oldConfig = getChannelConfiguration(configuration.getName());
        channelValidator.validate(configuration, false);
        channelConfigDao.updateChannel(configuration);
        if (configuration.isReplicating()) {
            replicator.notifyWatchers();
        } else if (oldConfig != null && oldConfig.isReplicating()) {
            replicator.notifyWatchers();
        }
        return configuration;
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery timeQuery) {
        Collection<ContentKey> keys = contentService.queryByTime(timeQuery);
        if (timeQuery.isStable()) {
            DateTime stableTime = TimeUtil.stable();
            return keys.stream()
                    .filter(key -> key.getTime().isBefore(stableTime))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
        return keys;
    }

    @Override
    public Collection<ContentKey> getKeys(DirectionQuery query) {
        if (query.getCount() <= 0) {
            query.getTraces().add("requested zero");
            return Collections.emptySet();
        }
        DateTime stableTime = TimeUtil.time(query.isStable());
        List<ContentKey> keys = new ArrayList<>(contentService.getKeys(query));
        if (query.isNext()) {
            return keys.stream()
                    .filter(key -> key.compareTo(query.getContentKey()) > 0)
                    .filter(key -> key.getTime().isBefore(stableTime))
                    .limit(query.getCount())
                    .collect(Collectors.toCollection(TreeSet::new));
        } else {
            Collection<ContentKey> contentKeys = new TreeSet<>(Collections.reverseOrder());
            contentKeys.addAll(keys);
            return contentKeys.stream()
                    .filter(key -> key.compareTo(query.getContentKey()) < 0)
                    .limit(query.getCount())
                    .collect(Collectors.toCollection(TreeSet::new));

        }
    }

    @Override
    public boolean delete(String channelName) {
        if (!channelConfigDao.channelExists(channelName)) {
            return false;
        }
        boolean replicating = isReplicating(channelName);
        contentService.delete(channelName);
        channelConfigDao.delete(channelName);
        if (replicating) {
            replicator.notifyWatchers();
        }
        return true;
    }
}
