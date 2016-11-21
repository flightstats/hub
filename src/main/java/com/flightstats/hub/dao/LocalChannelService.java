package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.DataDog;
import com.flightstats.hub.metrics.MetricsSender;
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

@Singleton
public class LocalChannelService implements ChannelService {
    public static final String REPLICATED_LAST_UPDATED = "/ReplicatedLastUpdated/";

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
            channelConfigDao.upsert(configuration);
            notify(configuration, oldConfig);
        } else {
            logger.info("update with no changes {}", configuration);
        }
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
    public boolean historicalInsert(String channelName, Content content) throws Exception {
        if (!isHistorical(channelName)) {
            logger.warn("historical inserts require a mutableTime on the channel. {}", channelName);
            throw new ForbiddenRequestException("historical inserts require a mutableTime on the channel.");
        }
        ChannelConfig channelConfig = getCachedChannelConfig(channelName);
        ContentKey contentKey = content.getContentKey().get();
        if (contentKey.getTime().isAfter(channelConfig.getMutableTime())) {
            String msg = "historical inserts must not be after mutableTime" + channelName + " " + contentKey;
            logger.warn(msg);
            throw new InvalidRequestException(msg);
        }
        boolean insert = contentService.historicalInsert(channelName, content);
        //todo gfm - send stats
        return insert;
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
    public Optional<ContentKey> getLatest(DirectionQuery query) {
        String channel = query.getChannelName();
        if (!channelExists(channel)) {
            return Optional.absent();
        }
        query = query.withStartKey(ContentKey.lastKey(TimeUtil.now().plusMinutes(1)));
        query = configureQuery(query);
        Optional<ContentKey> latest = contentService.getLatest(query);
        if (latest.isPresent()) {
            SortedSet<ContentKey> filtered = ContentKeyUtil.filter(latest.asSet(), query);
            if (filtered.isEmpty()) {
                return Optional.absent();
            }
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
        DateTime limitTime = getChannelLimitTime(request.getChannel()).minusMinutes(15);
        if (request.getKey().getTime().isBefore(limitTime)) {
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
        query = query.withChannelConfig(getCachedChannelConfig(query.getChannelName()));
        Stream<ContentKey> stream = contentService.queryByTime(query).stream();
        stream = ContentKeyUtil.enforceLimits(query, stream);
        return stream.collect(Collectors.toCollection(TreeSet::new));
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        if (query.getCount() <= 0) {
            return Collections.emptySortedSet();
        }
        query = configureQuery(query);
        List<ContentKey> keys = new ArrayList<>(contentService.queryDirection(query));
        SortedSet<ContentKey> contentKeys = ContentKeyUtil.filter(keys, query);
        ActiveTraces.getLocal().add("ChannelService.query", contentKeys);
        return contentKeys;
    }

    private DirectionQuery configureQuery(DirectionQuery query) {
        ActiveTraces.getLocal().add("configureQuery.start", query);
        if (query.getCount() > DIR_COUNT_LIMIT) {
            query = query.withCount(DIR_COUNT_LIMIT);
        }
        ChannelConfig channelConfig = getCachedChannelConfig(query.getChannelName());
        query = query.withChannelConfig(channelConfig);
        DateTime ttlTime = getChannelTtl(channelConfig, query.getEpoch());
        query = query.withEarliestTime(ttlTime);

        if (query.getStartKey().getTime().isBefore(ttlTime)) {
            query = query.withStartKey(new ContentKey(ttlTime, "0"));
        }
        if (query.getEpoch().equals(Epoch.MUTABLE)) {
            if (!query.isNext()) {
                DateTime mutableTime = channelConfig.getMutableTime();
                if (query.getStartKey().getTime().isAfter(mutableTime)) {
                    query = query.withStartKey(ContentKey.lastKey(mutableTime));
                }
            }
        }
        ContentPath lastUpdated = getLastUpdated(query.getChannelName(), new ContentKey(TimeUtil.time(query.isStable())));
        query = query.withChannelStable(lastUpdated.getTime());
        ActiveTraces.getLocal().add("configureQuery.end", query);
        return query;
    }

    private DateTime getChannelTtl(ChannelConfig channelConfig, Epoch epoch) {
        DateTime ttlTime = channelConfig.getTtlTime();
        if (channelConfig.isHistorical()) {
            if (epoch.equals(Epoch.IMMUTABLE)) {
                ttlTime = channelConfig.getMutableTime();
            } else {
                //todo gfm - this may need to be more sophisticated
                ttlTime = TimeUtil.BIG_BANG;
            }
        }
        return ttlTime;
    }

    @Override
    public void get(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        contentService.get(channel, keys, callback);
    }

    private DateTime getChannelLimitTime(String channelName) {
        ChannelConfig channelConfig = getCachedChannelConfig(channelName);
        if (channelConfig.isHistorical()) {
            return TimeUtil.BIG_BANG;
        }
        return channelConfig.getTtlTime();
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

        return true;
    }

    @Override
    public ContentPath getLastUpdated(String channelName, ContentPath defaultValue) {
        if (isReplicating(channelName)) {
            return lastContentPath.get(channelName, defaultValue, REPLICATED_LAST_UPDATED);
        }
        return defaultValue;
    }
}
