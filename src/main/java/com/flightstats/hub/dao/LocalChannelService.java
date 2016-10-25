package com.flightstats.hub.dao;

import com.diffplug.common.base.Errors;
import com.diffplug.common.base.Throwing;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.DataDog;
import com.flightstats.hub.metrics.MetricsSender;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ReplicationGlobalManager;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.webhook.WebhookService;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.timgroup.statsd.StatsDClient;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.isNull;

@Singleton
public class LocalChannelService implements ChannelService {
    public static final String REPLICATED_LAST_UPDATED = "/ReplicatedLastUpdated/";
    public static final String HISTORICAL_LAST_UPDATED = "/HistoricalLastUpdated/";
    public static final String HISTORICAL_FIRST_UPDATED = "/HistoricalFirstUpdated/";

    private final static Logger logger = LoggerFactory.getLogger(LocalChannelService.class);
    private final static StatsDClient statsd = DataDog.statsd;
    private static final int DIR_COUNT_LIMIT = HubProperties.getProperty("app.directionCountLimit", 10000);
    @Inject
    private ContentService contentService;
    @Inject
    @Named("ChannelConfig")
    private Dao<ChannelConfig> channelConfigDao;
    @Inject
    private ChannelValidator channelValidator;
    @Inject
    private ReplicationGlobalManager replicationGlobalManager;
    @Inject
    private MetricsSender sender;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private WebhookService webhookService;

    @Override
    public boolean channelExists(String channelName) {
        return channelConfigDao.exists(channelName);
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig configuration) {
        logger.info("create channel {}", configuration);
        channelValidator.validate(configuration, null, false);
        initializeHistorical(configuration);
        channelConfigDao.upsert(configuration);
        notify(configuration, null);
        return configuration;
    }

    private void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        if (newConfig.isReplicating() || newConfig.isGlobalMaster()) {
            replicationGlobalManager.notifyWatchers();
        } else if (oldConfig != null) {
            if (oldConfig.isReplicating() || oldConfig.isGlobalMaster()) {
                replicationGlobalManager.notifyWatchers();
            }
        }
        contentService.notify(newConfig, oldConfig);
    }

    @Override
    public ChannelConfig updateChannel(ChannelConfig configuration, ChannelConfig oldConfig, boolean isLocalHost) {
        if (!configuration.equals(oldConfig)) {
            logger.info("updating channel {} from {}", configuration, oldConfig);
            channelValidator.validate(configuration, oldConfig, isLocalHost);
            if (isNull(oldConfig)) {
                initializeHistorical(configuration);
            }
            channelConfigDao.upsert(configuration);
            notify(configuration, oldConfig);
        } else {
            logger.info("update with no changes {}", configuration);
        }
        return configuration;
    }

    private void initializeHistorical(ChannelConfig configuration) {
        if (configuration.isHistorical()) {
            lastContentPath.initialize(configuration.getName(), ContentKey.NONE, HISTORICAL_LAST_UPDATED);
            lastContentPath.initialize(configuration.getName(), ContentKey.NONE, HISTORICAL_FIRST_UPDATED);
        }
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        if (isHistorical(channelName)) {
            throw new ForbiddenRequestException("live inserts are not supported for historical channels.");
        }
        if (content.isNew() && isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        ContentKey contentKey = contentService.insert(channelName, content);
        long time = System.currentTimeMillis() - start;
        statsd.time("channel", time, "method:post", "type:single", "channel:" + channelName);
        statsd.increment("channel.items", "method:post", "type:single", "channel:" + channelName);
        statsd.count("channel.bytes", content.getSize(), "method:post", "type:single", "channel:" + channelName);
        sender.send("channel." + channelName + ".post", time);
        sender.send("channel." + channelName + ".items", 1);
        sender.send("channel." + channelName + ".post.bytes", content.getSize());
        sender.send("channel.ALL.post", time);
        return contentKey;
    }

    @Override
    public boolean historicalInsert(String channelName, Content content, boolean minuteComplete) throws Exception {
        if (!isHistorical(channelName)) {
            throw new ForbiddenRequestException("historical inserts are only supported for historical channels.");
        }
        Throwing.Function<ContentPath, ContentPath> inserter = existing -> {
            ContentKey insertKey = content.getContentKey().get();
            if (insertKey.compareTo(existing) <= 0) {
                throw new ConflictException("inserted item is not newer than existing item: " + existing);
            }
            if (contentService.historicalInsert(channelName, content)) {
                if (existing.equals(ContentKey.NONE)) {
                    lastContentPath.updateIncrease(insertKey, channelName, HISTORICAL_FIRST_UPDATED);
                    webhookService.unPauseHistorical(getCachedChannelConfig(channelName));
                }
                ContentPath nextPath = insertKey;
                if (minuteComplete) {
                    nextPath = new MinutePath(nextPath.getTime());
                }
                return nextPath;
            }
            return existing;
        };
        return lastContentPath.updateIncrease(channelName, HISTORICAL_LAST_UPDATED, Errors.rethrow().wrap(inserter));
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        String channel = bulkContent.getChannel();
        if (bulkContent.isNew() && isReplicating(channel)) {
            throw new ForbiddenRequestException(channel + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        Collection<ContentKey> contentKeys = contentService.insert(bulkContent);
        long time = System.currentTimeMillis() - start;
        statsd.time("channel", time, "method:post", "type:bulk", "channel:" + channel);
        statsd.count("channel.items", bulkContent.getItems().size(), "method:post", "type:bulk", "channel:" + channel);
        statsd.count("channel.bytes", bulkContent.getSize(), "method:post", "type:bulk", "channel:" + channel);
        sender.send("channel." + channel + ".batchPost", time);
        sender.send("channel." + channel + ".items", bulkContent.getItems().size());
        sender.send("channel." + channel + ".post", time);
        sender.send("channel." + channel + ".post.bytes", bulkContent.getSize());
        sender.send("channel.ALL.post", time);
        return contentKeys;
    }

    @Override
    public boolean isReplicating(String channelName) {
        try {
            return getCachedChannelConfig(channelName).isReplicating();
        } catch (NoSuchChannelException e) {
            return false;
        }
    }

    private boolean isHistorical(String channelName) {
        try {
            return getCachedChannelConfig(channelName).isHistorical();
        } catch (NoSuchChannelException e) {
            return false;
        }
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, boolean stable, boolean trace) {
        ChannelConfig channelConfig = getCachedChannelConfig(channel);
        if (null == channelConfig) {
            return Optional.absent();
        }
        Traces traces = ActiveTraces.getLocal();
        if (channelConfig.isHistorical()) {
            ContentPath lastUpdated = getLastUpdated(channel, ContentKey.NONE);
            traces.add("found historical last updated", lastUpdated);
            if (lastUpdated.equals(ContentKey.NONE)) {
                return Optional.absent();
            }
            return Optional.of((ContentKey) lastUpdated);
        }
        ContentKey limitKey = getLatestLimit(channel, stable);
        traces.add("get latest limit", limitKey);
        Optional<ContentKey> latest = contentService.getLatest(channel, limitKey, traces, stable);
        if (latest.isPresent()) {
            DateTime ttlTime = getTtlTime(channel);
            if (latest.get().getTime().isBefore(ttlTime)) {
                return Optional.absent();
            }
        }
        if (trace) {
            traces.log(logger);
        }
        return latest;
    }

    ContentKey getLatestLimit(String channelName, boolean stable) {
        ChannelConfig channel = getCachedChannelConfig(channelName);
        DateTime time = TimeUtil.now().plusMinutes(1);
        if (stable || !channel.isLive()) {
            time = getLastUpdated(channelName, new ContentKey(TimeUtil.stable())).getTime();
        }
        return ContentKey.lastKey(time);
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        contentService.deleteBefore(name, limitKey);
    }

    @Override
    public Optional<Content> get(Request request) {
        DateTime ttlTime = getTtlTime(request.getChannel()).minusMinutes(15);
        if (request.getKey().getTime().isBefore(ttlTime)) {
            return Optional.absent();
        }
        return contentService.get(request.getChannel(), request.getKey());
    }

    @Override
    public ChannelConfig getChannelConfig(String channelName, boolean allowChannelCache) {
        if (allowChannelCache) {
            return getCachedChannelConfig(channelName);
        }
        return channelConfigDao.get(channelName);
    }

    @Override
    public ChannelConfig getCachedChannelConfig(String channelName) {
        ChannelConfig channelConfig = channelConfigDao.getCached(channelName);
        if (null == channelConfig) {
            throw new NoSuchChannelException(channelName);
        }
        return channelConfig;
    }

    @Override
    public Collection<ChannelConfig> getChannels() {
        return getChannels(false);
    }

    private Collection<ChannelConfig> getChannels(boolean useCache) {
        return channelConfigDao.getAll(useCache);
    }

    @Override
    public Collection<ChannelConfig> getChannels(String tag, boolean useCache) {
        Collection<ChannelConfig> matchingChannels = new ArrayList<>();
        Iterable<ChannelConfig> channels = getChannels(useCache);
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
        Stream<ContentKey> stream = contentService.queryByTime(query).stream()
                .filter(key -> !key.getTime().isBefore(ttlTime));
        if (query.isStable()) {
            DateTime stableTime = TimeUtil.stable();
            stream = stream.filter(key -> key.getTime().isBefore(stableTime));
        }
        return stream.collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public SortedSet<ContentKey> getKeys(DirectionQuery query) {
        if (query.getCount() <= 0) {
            return Collections.emptySortedSet();
        }
        if (query.getCount() > DIR_COUNT_LIMIT) {
            query = query.withCount(DIR_COUNT_LIMIT);
        }
        DateTime ttlTime = getTtlTime(query.getChannelName());
        if (query.getContentKey().getTime().isBefore(ttlTime)) {
            query = query.withContentKey(new ContentKey(ttlTime, "0"));
        }
        query = query.withLiveChannel(getCachedChannelConfig(query.getChannelName()).isLive());
        query = query.withTtlTime(ttlTime);
        ContentPath lastUpdated = getLastUpdated(query.getChannelName(), new ContentKey(TimeUtil.time(query.isStable())));
        query = query.withChannelStable(lastUpdated.getTime());
        Traces traces = ActiveTraces.getLocal();
        traces.add(query);
        List<ContentKey> keys = new ArrayList<>(contentService.queryDirection(query));
        SortedSet<ContentKey> contentKeys = ContentKeyUtil.filter(keys, query.getContentKey(), ttlTime, query.getCount(), query.isNext(), query.isStable());
        traces.add("ChannelServiceImpl.getKeys", contentKeys);
        return contentKeys;
    }

    @Override
    public void get(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        contentService.get(channel, keys, callback);
    }

    private DateTime getTtlTime(String channelName) {
        ChannelConfig channel = getCachedChannelConfig(channelName);
        if (channel.isHistorical()) {
            return lastContentPath.get(channelName, new ContentKey(), HISTORICAL_FIRST_UPDATED).getTime();
        }
        return channel.getTtlTime();
    }

    @Override
    public boolean delete(String channelName) {
        if (!channelConfigDao.exists(channelName)) {
            return false;
        }
        ChannelConfig channelConfig = getCachedChannelConfig(channelName);
        contentService.delete(channelName);
        channelConfigDao.delete(channelName);
        if (channelConfig.isReplicating()) {
            replicationGlobalManager.notifyWatchers();
            lastContentPath.delete(channelName, REPLICATED_LAST_UPDATED);
        }
        if (channelConfig.isHistorical()) {
            lastContentPath.delete(channelName, HISTORICAL_LAST_UPDATED);
            lastContentPath.delete(channelName, HISTORICAL_FIRST_UPDATED);
        }
        return true;
    }

    @Override
    public ContentPath getLastUpdated(String channelName, ContentPath defaultValue) {
        if (isHistorical(channelName)) {
            return lastContentPath.get(channelName, defaultValue, HISTORICAL_LAST_UPDATED);
        }
        if (isReplicating(channelName)) {
            return lastContentPath.get(channelName, defaultValue, REPLICATED_LAST_UPDATED);
        }
        return defaultValue;
    }
}
