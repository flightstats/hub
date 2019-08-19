package com.flightstats.hub.dao;

import com.flightstats.hub.app.InFlightService;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.ClusterCacheDao;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.properties.ContentProperties;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.dao.aws.MultiPartParser;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.exception.MethodNotAllowedException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.StreamResults;
import com.flightstats.hub.time.TimeService;
import com.flightstats.hub.util.TimeUtil;
import com.flightstats.hub.webhook.TagWebhook;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static com.flightstats.hub.constant.ZookeeperNodes.HISTORICAL_EARLIEST;
import static com.flightstats.hub.constant.ZookeeperNodes.REPLICATED_LAST_UPDATED;
import static com.flightstats.hub.constant.ZookeeperNodes.REPLICATOR_WATCHER_PATH;

import static com.flightstats.hub.metrics.ChannelMetricTag.BULK;
import static com.flightstats.hub.metrics.ChannelMetricTag.HISTORICAL;
import static com.flightstats.hub.metrics.ChannelMetricTag.SINGLE;

@Singleton
@Slf4j
public class ChannelService {
    private final ContentService contentService;
    private final Dao<ChannelConfig> channelConfigDao;
    private final Provider<ChannelValidator> channelValidator;
    private final WatchManager watchManager;
    private final ClusterCacheDao clusterCacheDao;
    private final InFlightService inFlightService;
    private final TimeService timeService;
    private final StatsdReporter statsdReporter;
    private final ContentRetriever contentRetriever;
    private final ContentProperties contentProperties;

    @Inject
    private TagWebhook tagWebhook;

    @Inject
    public ChannelService(
            ContentService contentService,
            @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
            Provider<ChannelValidator> channelValidator,
            WatchManager watchManager,
            ClusterCacheDao clusterCacheDao,
            InFlightService inFlightService,
            TimeService timeService,
            StatsdReporter statsdReporter,
            ContentRetriever contentRetriever,
            ContentProperties contentProperties) {
        this.contentService = contentService;
        this.channelConfigDao = channelConfigDao;
        this.channelValidator = channelValidator;
        this.watchManager = watchManager;
        this.clusterCacheDao = clusterCacheDao;
        this.inFlightService = inFlightService;
        this.timeService = timeService;
        this.statsdReporter = statsdReporter;
        this.contentRetriever = contentRetriever;
        this.contentProperties = contentProperties;
    }

    public ChannelConfig createChannel(ChannelConfig configuration) {
        log.debug("create channel {}", configuration);
        channelValidator.get().validate(configuration, null, false);
        channelConfigDao.upsert(configuration);
        notify(configuration, null);
        tagWebhook.updateTagWebhooksDueToChannelConfigChange(configuration);
        return configuration;
    }

    private void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        if (newConfig.isReplicating()) {
            notifyReplicationWatchers();
        } else if (oldConfig != null) {
            if (oldConfig.isReplicating()) {
                notifyReplicationWatchers();
            }
        }
        if (newConfig.isHistorical()) {
            if (oldConfig == null || !oldConfig.isHistorical()) {
                ContentKey lastKey = ContentKey.lastKey(newConfig.getMutableTime());
                clusterCacheDao.set(lastKey, newConfig.getDisplayName(), HISTORICAL_EARLIEST);
            }
        }
        contentService.notify(newConfig, oldConfig);
    }

    public ChannelConfig updateChannel(ChannelConfig configuration, ChannelConfig oldConfig, boolean isLocalHost) {
        if (!configuration.equals(oldConfig)) {
            log.debug("updating channel {} from {}", configuration, oldConfig);
            channelValidator.get().validate(configuration, oldConfig, isLocalHost);
            channelConfigDao.upsert(configuration);
            tagWebhook.updateTagWebhooksDueToChannelConfigChange(configuration);
            notify(configuration, oldConfig);
        } else {
            log.debug("update with no changes {}", configuration);
        }
        return configuration;
    }

    public ContentKey insert(String channelName, Content content) {
        channelName = contentRetriever.getDisplayName(channelName);
        if (content.isNew() && contentRetriever.isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        ContentKey contentKey = insertInternal(channelName, content);
        statsdReporter.insert(channelName, start, SINGLE, 1, content.getSize());
        return contentKey;
    }

    private ContentKey insertInternal(String channelName, Content content) {
        ChannelConfig channelConfig = contentRetriever.getExpectedCachedChannelConfig(channelName);
        return inFlightService.inFlight(() -> {
            Traces traces = ActiveTraces.getLocal();
            traces.add("ContentService.insert");
            try {
                content.packageStream();
                checkZeroBytes(content, channelConfig);
                traces.add("ContentService.insert marshalled");
                ContentKey key = content.keyAndStart(timeService.getNow());
                log.debug("writing key {} to channel {}", key, channelName);
                key = contentService.insert(channelName, content);
                traces.add("ContentService.insert end", key);
                return key;
            } catch (ContentTooLargeException e) {
                log.error("content too large for channel " + channelName);
                throw e;
            } catch (Exception e) {
                traces.add("ContentService.insert", "error", e.getMessage());
                log.warn("insertion error " + channelName, e);
                throw e;
            }
        });
    }

    @SneakyThrows
    public boolean historicalInsert(String channelName, Content content) throws RuntimeException {
        String normalizedChannelName = contentRetriever.getDisplayName(channelName);
        if (!isHistorical(channelName)) {
            log.warn("historical inserts require a mutableTime on the channel. {}", normalizedChannelName);
            throw new ForbiddenRequestException("historical inserts require a mutableTime on the channel.");
        }
        long start = System.currentTimeMillis();

        ChannelConfig channelConfig = contentRetriever.getExpectedCachedChannelConfig(channelName);
        ContentKey contentKey = content.getContentKey()
                .orElseThrow(() -> {
                    throw new RuntimeException("internal error: invalid content key on historical insert to channel: " + channelName);
                });
        if (contentKey.getTime().isAfter(channelConfig.getMutableTime())) {
            String msg = "historical inserts must not be after mutableTime" + normalizedChannelName + " " + contentKey;
            log.warn(msg);
            throw new InvalidRequestException(msg);
        }
        boolean insert = inFlightService.inFlight(() -> {
            content.packageStream();
            checkZeroBytes(content, channelConfig);
            return contentService.historicalInsert(normalizedChannelName, content);
        });
        clusterCacheDao.setIfOlder(contentKey, normalizedChannelName, HISTORICAL_EARLIEST);
        statsdReporter.insert(normalizedChannelName, start, HISTORICAL, 1, content.getSize());
        return insert;
    }

    private void checkZeroBytes(Content content, ChannelConfig channelConfig) {
        if (!channelConfig.isAllowZeroBytes() && content.getContentLength() == 0) {
            throw new InvalidRequestException("zero byte items are not allowed in this channel");
        }
    }

    public Collection<ContentKey> insert(BulkContent content) {
        BulkContent bulkContent = content.withChannel(contentRetriever.getDisplayName(content.getChannel()));
        String channel = bulkContent.getChannel();
        if (bulkContent.isNew() && contentRetriever.isReplicating(channel)) {
            throw new ForbiddenRequestException(channel + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        Collection<ContentKey> contentKeys = inFlightService.inFlight(() -> {
            MultiPartParser multiPartParser = new MultiPartParser(bulkContent, contentProperties.getMaxPayloadSizeInMB());
            multiPartParser.parse();
            return contentService.insert(bulkContent);
        });
        statsdReporter.insert(channel, start, BULK, bulkContent.getItems().size(), bulkContent.getSize());
        return contentKeys;
    }

    private boolean isHistorical(String channelName) {
        return getCachedChannelConfig(channelName)
                .filter(ChannelConfig::isHistorical)
                .isPresent();
    }

    public void deleteBefore(String channel, ContentKey limitKey) {
        channel = contentRetriever.getDisplayName(channel);
        contentService.deleteBefore(channel, limitKey);
    }

    public Optional<Content> get(ItemRequest itemRequest) {
        itemRequest = itemRequest.withChannel(contentRetriever.getDisplayName(itemRequest.getChannel()));
        final DateTime limitTime = getChannelLimitTime(itemRequest.getChannel()).minusMinutes(15);
        if (itemRequest.getKey().getTime().isBefore(limitTime)) {
            return Optional.empty();
        }
        return contentService.get(itemRequest.getChannel(), itemRequest.getKey(), itemRequest.isRemoteOnly());
    }

    public Collection<ChannelConfig> getChannels() {
        return getChannels(false);
    }

    private Collection<ChannelConfig> getChannels(boolean useCache) {
        return channelConfigDao.getAll(useCache);
    }

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

    public Iterable<String> getTags() {
        Collection<String> matchingChannels = new HashSet<>();
        Iterable<ChannelConfig> channels = getChannels();
        for (ChannelConfig channel : channels) {
            matchingChannels.addAll(channel.getTags());
        }
        return matchingChannels;
    }

    public void get(StreamResults streamResults) {
        streamResults = streamResults.withChannel(contentRetriever.getDisplayName(streamResults.getChannel()));
        contentService.get(streamResults);
    }

    private DateTime getChannelLimitTime(String channelName) {
        ChannelConfig channelConfig = contentRetriever.getExpectedCachedChannelConfig(channelName);
        if (channelConfig.isHistorical()) {
            return TimeUtil.BIG_BANG;
        }
        return channelConfig.getTtlTime();
    }

    public boolean delete(String channelName) {
        channelName = contentRetriever.getDisplayName(channelName);
        Optional<ChannelConfig> optionalChannelConfig = contentRetriever.getCachedChannelConfig(channelName);
        if (!optionalChannelConfig.isPresent()) {
            return false;
        }
        ChannelConfig channelConfig = optionalChannelConfig.get();
        contentService.delete(channelConfig.getDisplayName());
        channelConfigDao.delete(channelConfig.getDisplayName());
        if (channelConfig.isReplicating()) {
            notifyReplicationWatchers();
            clusterCacheDao.delete(channelName, REPLICATED_LAST_UPDATED);
        }
        clusterCacheDao.delete(channelName, HISTORICAL_EARLIEST);
        tagWebhook.deleteAllTagWebhooksForChannel(channelConfig);
        return true;
    }

    public boolean delete(String channelName, ContentKey contentKey) {
        channelName = contentRetriever.getDisplayName(channelName);
        ChannelConfig channelConfig = contentRetriever.getExpectedCachedChannelConfig(channelName);
        if (channelConfig.isHistorical()) {
            if (!contentKey.getTime().isAfter(channelConfig.getMutableTime())) {
                contentService.delete(channelName, contentKey);
                return true;
            }
        }
        String message = "item is not within the channel's mutableTime";
        ActiveTraces.getLocal().add(message, channelName, contentKey);
        throw new MethodNotAllowedException(message);
    }

    public Optional<ChannelConfig> getChannelConfig(String channelName, boolean allowChannelCache) {
        if (allowChannelCache) {
            return getCachedChannelConfig(channelName);
        }
        return Optional.ofNullable(channelConfigDao.get(channelName));
    }

    private Optional<ChannelConfig> getCachedChannelConfig(String channelName) {
        ChannelConfig channelConfig = channelConfigDao.getCached(channelName);
        return Optional.ofNullable(channelConfig);
    }

    private void notifyReplicationWatchers() {
        watchManager.notifyWatcher(REPLICATOR_WATCHER_PATH);
    }

    public boolean refresh() {
        return channelConfigDao.refresh();
    }

}
