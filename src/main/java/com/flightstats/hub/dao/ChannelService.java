package com.flightstats.hub.dao;

import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ReplicatorManager;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ChannelService {
    private final static Logger logger = LoggerFactory.getLogger(ChannelService.class);

    @Inject
    private ContentService contentService;
    @Inject
    private ChannelConfigDao channelConfigDao;
    @Inject
    private ChannelValidator channelValidator;
    @Inject
    private ReplicatorManager replicatorManager;
    @Inject
    private MetricsSender sender;
    @Inject
    private LastContentPath lastContentPath;

    public static final String CHANNEL_LATEST_UPDATED = "/ChannelLatestUpdated/";

    public boolean channelExists(String channelName) {
        return channelConfigDao.channelExists(channelName);
    }

    public ChannelConfig createChannel(ChannelConfig configuration) {
        long start = System.currentTimeMillis();
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
        contentService.notify(newConfig, oldConfig);
    }

    public ChannelConfig updateChannel(ChannelConfig configuration, ChannelConfig oldConfig) {
        if (configuration.hasChanged(oldConfig)) {
            long start = System.currentTimeMillis();
            logger.info("updating channel {} from {}", configuration, oldConfig);
            configuration = ChannelConfig.builder().withChannelConfiguration(configuration).build();
            channelValidator.validate(configuration, false);
            channelConfigDao.updateChannel(configuration);
            notify(configuration, oldConfig);
        } else {
            logger.info("update with no changes {}", configuration);
        }
        return configuration;
    }

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

    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        String channel = bulkContent.getChannel();
        if (bulkContent.isNew() && isReplicating(channel)) {
            throw new ForbiddenRequestException(channel + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        Collection<ContentKey> contentKeys = contentService.insert(bulkContent);
        long time = System.currentTimeMillis() - start;
        sender.send("channel." + channel + ".batchPost", time);
        sender.send("channel." + channel + ".items", bulkContent.getItems().size());
        sender.send("channel." + channel + ".post", time);
        sender.send("channel." + channel + ".post.bytes", bulkContent.getSize());
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

    public Optional<ContentKey> getLatest(String channel, boolean stable, boolean trace) {
        ChannelConfig channelConfig = getCachedChannelConfig(channel);
        if (null == channelConfig) {
            return Optional.absent();
        }
        Traces traces = ActiveTraces.getLocal();
        DateTime time = TimeUtil.stable();
        if (!stable) {
            //if not stable, we don't want to miss any results.
            time = TimeUtil.now().plusMinutes(1);
        }
        ContentKey limitKey = new ContentKey(time, "ZZZZZZ");
        Optional<ContentKey> latest = contentService.getLatest(channel, limitKey, traces);
        if (latest.isPresent()) {
            lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            DateTime ttlTime = getTtlTime(channel);
            if (latest.get().getTime().isBefore(ttlTime)) {
                return Optional.absent();
            }
            if (trace) {
                traces.log(logger);
            }
            return latest;
        }
        ContentPath latestCache = lastContentPath.get(channel, null, CHANNEL_LATEST_UPDATED);
        if (latestCache != null) {
            if (latestCache.equals(ContentKey.NONE)) {
                return Optional.absent();
            }
            return Optional.of((ContentKey) latestCache);
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
            lastContentPath.updateIncrease(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            return Optional.absent();
        } else {
            ContentKey latestKey = keys.iterator().next();
            lastContentPath.updateIncrease(latestKey, channel, CHANNEL_LATEST_UPDATED);
            return Optional.of(latestKey);
        }
    }

    public void deleteBefore(String name, ContentKey limitKey) {
        contentService.deleteBefore(name, limitKey);
    }

    public Optional<Content> getValue(Request request) {
        DateTime ttlTime = getTtlTime(request.getChannel()).minusMinutes(15);
        if (request.getKey().getTime().isBefore(ttlTime)) {
            return Optional.absent();
        }
        return contentService.getValue(request.getChannel(), request.getKey());
    }

    public ChannelConfig getChannelConfig(String channelName, boolean allowChannelCache) {
        if (allowChannelCache) {
            return getCachedChannelConfig(channelName);
        }
        return channelConfigDao.getChannelConfig(channelName);
    }

    public ChannelConfig getCachedChannelConfig(String channelName) {
        ChannelConfig channelConfig = channelConfigDao.getCachedChannelConfig(channelName);
        if (null == channelConfig) {
            throw new NoSuchChannelException(channelName);
        }
        return channelConfig;
    }

    public Iterable<ChannelConfig> getChannels() {
        return channelConfigDao.getChannels();
    }

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

    public Iterable<String> getTags() {
        Collection<String> matchingChannels = new HashSet<>();
        Iterable<ChannelConfig> channels = getChannels();
        for (ChannelConfig channel : channels) {
            matchingChannels.addAll(channel.getTags());
        }
        return matchingChannels;
    }

    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        if (query == null) {
            return Collections.emptySortedSet();
        }
        DateTime ttlTime = getTtlTime(query.getChannelName());
        Stream<ContentKey> stream = contentService.queryByTime(query).stream()
                .filter(key -> key.getTime().isAfter(ttlTime));
        if (query.isStable()) {
            DateTime stableTime = TimeUtil.stable();
            stream = stream.filter(key -> key.getTime().isBefore(stableTime));
        }
        return stream.collect(Collectors.toCollection(TreeSet::new));
    }

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
        //todo - gfm - 2/24/16 - returns all of the keys found
        List<ContentKey> keys = new ArrayList<>(contentService.queryDirection(query));
        //todo - gfm - 2/24/16 - filter down to one for latest
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

    public boolean delete(String channelName) {
        if (!channelConfigDao.channelExists(channelName)) {
            return false;
        }
        long start = System.currentTimeMillis();
        boolean replicating = isReplicating(channelName);
        contentService.delete(channelName);
        channelConfigDao.delete(channelName);
        if (replicating) {
            replicatorManager.notifyWatchers();
        }
        return true;
    }
}
