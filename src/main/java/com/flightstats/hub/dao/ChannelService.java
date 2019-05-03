package com.flightstats.hub.dao;

import com.flightstats.hub.app.InFlightService;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.ContentProperties;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.dao.aws.MultiPartParser;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.exception.MethodNotAllowedException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.ChannelType;
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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import static com.flightstats.hub.util.Constants.HISTORICAL_EARLIEST;
import static com.flightstats.hub.util.Constants.REPLICATED_LAST_UPDATED;
import static com.flightstats.hub.util.Constants.REPLICATOR_WATCHER_PATH;

@Singleton
@Slf4j
public class ChannelService {

    private final ContentService contentService;
    private final Dao<ChannelConfig> channelConfigDao;
    private final ChannelValidator channelValidator;
    private final WatchManager watchManager;
    private final LastContentPath lastContentPath;
    private final InFlightService inFlightService;
    private final TimeService timeService;
    private final StatsdReporter statsdReporter;
    private final ContentRetriever contentRetriever;
    private ContentProperties contentProperties;

    @Inject
    private TagWebhook tagWebhook;

    @Inject
    public ChannelService(ContentService contentService,
                          @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                          ChannelValidator channelValidator,
                          WatchManager watchManager,
                          LastContentPath lastContentPath,
                          InFlightService inFlightService,
                          TimeService timeService,
                          StatsdReporter statsdReporter,
                          ContentRetriever contentRetriever,
                          ContentProperties contentProperties) {
        this.contentService = contentService;
        this.channelConfigDao = channelConfigDao;
        this.channelValidator = channelValidator;
        this.watchManager = watchManager;
        this.lastContentPath = lastContentPath;
        this.inFlightService = inFlightService;
        this.timeService = timeService;
        this.statsdReporter = statsdReporter;
        this.contentRetriever = contentRetriever;
        this.contentProperties = contentProperties;
    }

    public ChannelConfig createChannel(ChannelConfig configuration) {
        log.info("create channel {}", configuration);
        channelValidator.validate(configuration, null, false);
        channelConfigDao.upsert(configuration);
        notify(configuration, null);
        tagWebhook.updateTagWebhooksDueToChannelConfigChange(configuration);
        return configuration;
    }

    private void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        if (newConfig.isReplicating()) {
            watchManager.notifyWatcher(REPLICATOR_WATCHER_PATH);
        } else if (oldConfig != null) {
            if (oldConfig.isReplicating()) {
                watchManager.notifyWatcher(REPLICATOR_WATCHER_PATH);
            }
        }
        if (newConfig.isHistorical()) {
            if (oldConfig == null || !oldConfig.isHistorical()) {
                ContentKey lastKey = ContentKey.lastKey(newConfig.getMutableTime());
                lastContentPath.update(lastKey, newConfig.getDisplayName(), HISTORICAL_EARLIEST);
            }
        }
        contentService.notify(newConfig, oldConfig);
    }

    public ChannelConfig updateChannel(ChannelConfig configuration, ChannelConfig oldConfig, boolean isLocalHost) {
        if (!configuration.equals(oldConfig)) {
            log.info("updating channel {} from {}", configuration, oldConfig);
            channelValidator.validate(configuration, oldConfig, isLocalHost);
            channelConfigDao.upsert(configuration);
            tagWebhook.updateTagWebhooksDueToChannelConfigChange(configuration);
            notify(configuration, oldConfig);
        } else {
            log.info("update with no changes {}", configuration);
        }
        return configuration;
    }

    public ContentKey insert(String channelName, Content content) {
        channelName = contentRetriever.getDisplayName(channelName);
        if (content.isNew() && contentRetriever.isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
        final long start = System.currentTimeMillis();
        final ContentKey contentKey = insertInternal(channelName, content);
        statsdReporter.insert(channelName, start, ChannelType.SINGLE, 1, content.getSize());
        return contentKey;
    }

    private ContentKey insertInternal(String channelName, Content content) {
        final ChannelConfig channelConfig = contentRetriever.getExpectedCachedChannelConfig(channelName);
        return inFlightService.inFlight(() -> {
            Traces traces = ActiveTraces.getLocal();
            traces.add("ContentService.insert");
            try {
                content.packageStream();
                checkZeroBytes(content, channelConfig);
                traces.add("ContentService.insert marshalled");
                ContentKey key = content.keyAndStart(timeService.getNow());
                log.trace("writing key {} to channel {}", key, channelName);
                key = contentService.insert(channelName, content);
                traces.add("ContentService.insert end", key);
                return key;
            } catch (ContentTooLargeException e) {
                log.info("content too large for channel " + channelName);
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
        final String normalizedChannelName = contentRetriever.getDisplayName(channelName);
        if (!isHistorical(channelName)) {
            log.warn("historical inserts require a mutableTime on the channel. {}", normalizedChannelName);
            throw new ForbiddenRequestException("historical inserts require a mutableTime on the channel.");
        }
        long start = System.currentTimeMillis();

        final ChannelConfig channelConfig = contentRetriever.getExpectedCachedChannelConfig(channelName);
        final ContentKey contentKey = content.getContentKey()
                .orElseThrow(() -> {
                    throw new RuntimeException("internal error: invalid content key on historical insert to channel: " + channelName);
                });
        if (contentKey.getTime().isAfter(channelConfig.getMutableTime())) {
            String msg = "historical inserts must not be after mutableTime" + normalizedChannelName + " " + contentKey;
            log.warn(msg);
            throw new InvalidRequestException(msg);
        }
        final boolean insert = inFlightService.inFlight(() -> {
            content.packageStream();
            checkZeroBytes(content, channelConfig);
            return contentService.historicalInsert(normalizedChannelName, content);
        });
        lastContentPath.updateDecrease(contentKey, normalizedChannelName, HISTORICAL_EARLIEST);
        statsdReporter.insert(normalizedChannelName, start, ChannelType.HISTORICAL, 1, content.getSize());
        return insert;
    }

    private void checkZeroBytes(Content content, ChannelConfig channelConfig) {
        if (!channelConfig.isAllowZeroBytes() && content.getContentLength() == 0) {
            throw new InvalidRequestException("zero byte items are not allowed in this channel");
        }
    }

    public Collection<ContentKey> insert(BulkContent content) {
        final BulkContent bulkContent = content.withChannel(contentRetriever.getDisplayName(content.getChannel()));
        final String channel = bulkContent.getChannel();
        if (bulkContent.isNew() && contentRetriever.isReplicating(channel)) {
            throw new ForbiddenRequestException(channel + " cannot modified while replicating");
        }
        long start = System.currentTimeMillis();
        final Collection<ContentKey> contentKeys = inFlightService.inFlight(() -> {
            MultiPartParser multiPartParser = new MultiPartParser(bulkContent, contentProperties.getMaxPayloadSizeInMB());
            multiPartParser.parse();
            return contentService.insert(bulkContent);
        });
        statsdReporter.insert(channel, start, ChannelType.BULK, bulkContent.getItems().size(), bulkContent.getSize());
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
        final Collection<ChannelConfig> matchingChannels = new ArrayList<>();
        final Iterable<ChannelConfig> channels = getChannels(useCache);
        for (ChannelConfig channel : channels) {
            if (channel.getTags().contains(tag)) {
                matchingChannels.add(channel);
            }
        }
        return matchingChannels;
    }

    public Iterable<String> getTags() {
        final Collection<String> matchingChannels = new HashSet<>();
        final Iterable<ChannelConfig> channels = getChannels();
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
        final ChannelConfig channelConfig = contentRetriever.getExpectedCachedChannelConfig(channelName);
        if (channelConfig.isHistorical()) {
            return TimeUtil.BIG_BANG;
        }
        return channelConfig.getTtlTime();
    }

    public boolean delete(String channelName) {
        channelName = contentRetriever.getDisplayName(channelName);
        final Optional<ChannelConfig> optionalChannelConfig = contentRetriever.getCachedChannelConfig(channelName);
        if (!optionalChannelConfig.isPresent()) {
            return false;
        }
        final ChannelConfig channelConfig = optionalChannelConfig.get();
        contentService.delete(channelConfig.getDisplayName());
        channelConfigDao.delete(channelConfig.getDisplayName());
        if (channelConfig.isReplicating()) {
            watchManager.notifyWatcher(REPLICATOR_WATCHER_PATH);
            lastContentPath.delete(channelName, REPLICATED_LAST_UPDATED);
        }
        lastContentPath.delete(channelName, HISTORICAL_EARLIEST);
        tagWebhook.deleteAllTagWebhooksForChannel(channelConfig);
        return true;
    }

    public boolean delete(String channelName, ContentKey contentKey) {
        channelName = contentRetriever.getDisplayName(channelName);
        final ChannelConfig channelConfig = contentRetriever.getExpectedCachedChannelConfig(channelName);
        if (channelConfig.isHistorical()) {
            if (!contentKey.getTime().isAfter(channelConfig.getMutableTime())) {
                contentService.delete(channelName, contentKey);
                return true;
            }
        }
        final String message = "item is not within the channels mutableTime";
        ActiveTraces.getLocal().add(message, channelName, contentKey);
        throw new MethodNotAllowedException(message);
    }

    public Optional<ChannelConfig> getChannelConfig(String channelName, boolean allowChannelCache) {
        if (allowChannelCache) {
            return getCachedChannelConfig(channelName);
        }
        return Optional.ofNullable(channelConfigDao.get(channelName));
    }

    public Optional<ChannelConfig> getCachedChannelConfig(String channelName) {
        ChannelConfig channelConfig = channelConfigDao.getCached(channelName);
        return Optional.ofNullable(channelConfig);
    }
}
