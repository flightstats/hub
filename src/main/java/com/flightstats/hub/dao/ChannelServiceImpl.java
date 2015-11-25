package com.flightstats.hub.dao;

import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ReplicatorManager;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ChannelServiceImpl implements ChannelService {
    private final static Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final ContentService contentService;
    private final ChannelConfigDao channelConfigDao;
    private final ChannelValidator channelValidator;
    private final ReplicatorManager replicatorManager;
    private MetricsSender sender;
    private final HubUtils hubUtils;

    @Inject
    public ChannelServiceImpl(ContentService contentService, ChannelConfigDao channelConfigDao,
                              ChannelValidator channelValidator, ReplicatorManager replicatorManager,
                              MetricsSender sender, HubUtils hubUtils) {
        this.contentService = contentService;
        this.channelConfigDao = channelConfigDao;
        this.channelValidator = channelValidator;
        this.replicatorManager = replicatorManager;
        this.sender = sender;
        this.hubUtils = hubUtils;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelConfigDao.channelExists(channelName);
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig configuration) {
        logger.info("create channel {}", configuration);
        channelValidator.validate(configuration, true);
        configuration = ChannelConfig.builder().withChannelConfiguration(configuration).build();
        ChannelConfig created = channelConfigDao.createChannel(configuration);
        notify(created, null);
        return created;
    }

    private void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        if (newConfig.isReplicating()) {
            replicatorManager.notifyWatchers();
        } else if (oldConfig != null && oldConfig.isReplicating()) {
            replicatorManager.notifyWatchers();
        }
        if (newConfig.isSingle()) {
            if (oldConfig != null && !oldConfig.isSingle()) {
                new S3Batch(newConfig, hubUtils).stop();
            }
        } else {
            new S3Batch(newConfig, hubUtils).start();
        }
    }

    @Override
    public ChannelConfig updateChannel(ChannelConfig configuration) {
        logger.info("updating channel {}", configuration);
        configuration = ChannelConfig.builder().withChannelConfiguration(configuration).build();
        ChannelConfig oldConfig = getChannelConfig(configuration.getName());
        channelValidator.validate(configuration, false);
        channelConfigDao.updateChannel(configuration);
        notify(configuration, oldConfig);
        return configuration;
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        if (content.isNew() && isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        ContentKey contentKey = contentService.insert(channelName, content);
        long time = System.currentTimeMillis() - start;
        sender.send("channel." + channelName + ".post", time);
        sender.send("channel." + channelName + ".items", 1);
        sender.send("channel." + channelName + ".post.bytes", content.getSize());
        sender.send("channel.ALL.post", time);
        return contentKey;
    }

    @Override
    public Collection<ContentKey> insert(String channelName, BulkContent bulkContent) throws Exception {
        if (bulkContent.isNew() && isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        Collection<ContentKey> contentKeys = contentService.insert(channelName, bulkContent);
        long time = System.currentTimeMillis() - start;
        sender.send("channel." + channelName + ".batchPost", time);
        sender.send("channel." + channelName + ".items", bulkContent.getItems().size());
        sender.send("channel." + channelName + ".post", time);
        sender.send("channel." + channelName + ".post.bytes", bulkContent.getSize());
        sender.send("channel.ALL.post", time);
        return contentKeys;
    }

    public boolean isReplicating(String channelName) {
        try {
            ChannelConfig configuration = getCachedChannelConfig(channelName);
            return configuration.isReplicating();
        } catch (NoSuchChannelException e) {
            return false;
        }
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, boolean stable, boolean trace) {
        ChannelConfig channelConfig = getChannelConfig(channel);
        if (null == channelConfig) {
            return Optional.absent();
        }
        Traces traces = ActiveTraces.getLocal();
        ContentKey limitKey = new ContentKey(TimeUtil.time(stable), "ZZZZZZ");
        Optional<ContentKey> latest = contentService.getLatest(channel, limitKey, traces);
        if (latest.isPresent()) {
            DateTime ttlTime = getTtlTime(channel);
            if (latest.get().getTime().isBefore(ttlTime)) {
                return Optional.absent();
            }
            if (trace) {
                traces.log(logger);
            }
            return latest;
        }

        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(limitKey)
                .next(false)
                .stable(stable)
                .ttlDays(channelConfig.getTtlDays())
                .count(1)
                .build();
        Collection<ContentKey> keys = getKeys(query);
        if (trace) {
            traces.log(logger);
        }
        if (keys.isEmpty()) {
            return Optional.absent();
        } else {
            return Optional.of(keys.iterator().next());
        }
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        contentService.deleteBefore(name, limitKey);
    }

    @Override
    public Optional<Content> getValue(Request request) {
        DateTime ttlTime = getTtlTime(request.getChannel()).minusMinutes(15);
        if (request.getKey().getTime().isBefore(ttlTime)) {
            return Optional.absent();
        }
        return contentService.getValue(request.getChannel(), request.getKey());
    }

    @Override
    public ChannelConfig getChannelConfig(String channelName) {
        return channelConfigDao.getChannelConfig(channelName);
    }

    @Override
    public ChannelConfig getCachedChannelConfig(String channelName) {
        ChannelConfig channelConfig = channelConfigDao.getCachedChannelConfig(channelName);
        if (null == channelConfig) {
            throw new NoSuchChannelException(channelName);
        }
        return channelConfig;
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
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        if (query == null) {
            return Collections.emptySortedSet();
        }
        DateTime ttlTime = getTtlTime(query.getChannelName());
        DateTime stableTime = TimeUtil.time(query.isStable());
        return contentService.queryByTime(query)
                .stream()
                .filter(key -> key.getTime().isBefore(stableTime))
                .filter(key -> key.getTime().isAfter(ttlTime))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public SortedSet<ContentKey> getKeys(DirectionQuery query) {
        if (query.getCount() <= 0) {
            return Collections.emptySortedSet();
        }
        DateTime ttlTime = getTtlTime(query.getChannelName());
        if (query.getContentKey().getTime().isBefore(ttlTime)) {
            query = query.withContentKey(new ContentKey(ttlTime, "0"));
        }
        query = query.withTtlDays(getTtlDays(query.getChannelName()));
        Traces traces = ActiveTraces.getLocal();
        traces.add(query);
        List<ContentKey> keys = new ArrayList<>(contentService.queryDirection(query));
        SortedSet<ContentKey> contentKeys = ContentKeyUtil.filter(keys, query.getContentKey(), ttlTime, query.getCount(), query.isNext(), query.isStable());
        traces.add("ChannelServiceImpl.getKeys", contentKeys);
        return contentKeys;
    }

    public void getValues(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        contentService.getValues(channel, keys, callback);
    }

    private DateTime getTtlTime(String channelName) {
        return TimeUtil.getEarliestTime(getTtlDays(channelName));
    }

    private int getTtlDays(String channelName) {
        return (int) getCachedChannelConfig(channelName).getTtlDays();
    }

    @Override
    public boolean delete(String channelName) {
        if (!channelConfigDao.channelExists(channelName)) {
            return false;
        }
        ChannelConfig channelConfig = getChannelConfig(channelName);
        if (!channelConfig.isSingle()) {
            new S3Batch(channelConfig, hubUtils).stop();
        }
        boolean replicating = isReplicating(channelName);
        contentService.delete(channelName);
        channelConfigDao.delete(channelName);
        if (replicating) {
            replicatorManager.notifyWatchers();
        }
        return true;
    }
}
