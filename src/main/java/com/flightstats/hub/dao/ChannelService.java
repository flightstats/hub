package com.flightstats.hub.dao;

import com.flightstats.hub.app.InFlightService;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.config.ContentProperties;
import com.flightstats.hub.dao.aws.MultiPartParser;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.ForbiddenRequestException;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.exception.MethodNotAllowedException;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.ChannelType;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.model.StreamResults;
import com.flightstats.hub.model.TimeQuery;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.flightstats.hub.util.Constants.REPLICATED_LAST_UPDATED;
import static com.flightstats.hub.util.Constants.REPLICATOR_WATCHER_PATH;

@Singleton
@Slf4j
public class ChannelService {

    private static final String HISTORICAL_EARLIEST = "/HistoricalEarliest/";

    @Inject
    private ContentService contentService;

    private final Dao<ChannelConfig> channelConfigDao;
    private final ChannelValidator channelValidator;
    private final WatchManager watchManager;
    private final LastContentPath lastContentPath;
    private final InFlightService inFlightService;
    private final TimeService timeService;
    private final StatsdReporter statsdReporter;
    private final ContentProperties contentProperties;


  private TagWebhook tagWebhook;

    @Inject
    public ChannelService(//ContentService contentService,
                          @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                          ChannelValidator channelValidator,
                          WatchManager watchManager,
                          LastContentPath lastContentPath,
                          InFlightService inFlightService,
                          TimeService timeService,
                          StatsdReporter statsdReporter,
                          ContentProperties contentProperties,
                          TagWebhook tagWebhook) {
     //   this.contentService = contentService;
        this.channelConfigDao = channelConfigDao;
        this.channelValidator = channelValidator;
        this.watchManager = watchManager;
        this.lastContentPath = lastContentPath;
        this.inFlightService = inFlightService;
        this.timeService = timeService;
        this.statsdReporter = statsdReporter;
        this.contentProperties = contentProperties;
        this.tagWebhook = tagWebhook;
    }

    public boolean channelExists(String channelName) {
        return channelConfigDao.exists(channelName);
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

    public ContentKey insert(String channelName, Content content) throws Exception {
        channelName = getDisplayName(channelName);
        if (content.isNew() && isReplicating(channelName)) {
            throw new ForbiddenRequestException(channelName + " cannot modified while replicating");
        }
        final long start = System.currentTimeMillis();
        final ContentKey contentKey = insertInternal(channelName, content);
        statsdReporter.insert(channelName, start, ChannelType.SINGLE, 1, content.getSize());
        return contentKey;
    }

    private ContentKey insertInternal(String channelName, Content content) throws Exception {
        final ChannelConfig channelConfig = getExpectedCachedChannelConfig(channelName);
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
        final String normalizedChannelName = getDisplayName(channelName);
        if (!isHistorical(channelName)) {
            log.warn("historical inserts require a mutableTime on the channel. {}", normalizedChannelName);
            throw new ForbiddenRequestException("historical inserts require a mutableTime on the channel.");
        }
        long start = System.currentTimeMillis();

        final ChannelConfig channelConfig = getExpectedCachedChannelConfig(channelName);
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

    public Collection<ContentKey> insert(BulkContent content) throws Exception {
        final BulkContent bulkContent = content.withChannel(getDisplayName(content.getChannel()));
        final String channel = bulkContent.getChannel();
        if (bulkContent.isNew() && isReplicating(channel)) {
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

    public boolean isReplicating(String channelName) {
        return getCachedChannelConfig(channelName)
                .filter(ChannelConfig::isReplicating)
                .isPresent();
    }

    private boolean isHistorical(String channelName) {
        return getCachedChannelConfig(channelName)
                .filter(ChannelConfig::isHistorical)
                .isPresent();
    }

    public Optional<ContentKey> getLatest(DirectionQuery query) {
        query = query.withChannelName(getDisplayName(query.getChannelName()));
        final String channel = query.getChannelName();
        if (!channelExists(channel)) {
            return Optional.empty();
        }
        query = query.withStartKey(getLatestLimit(query.getChannelName(), query.isStable()));
        query = configureQuery(query);
        final Optional<ContentKey> latest = contentService.getLatest(query);
        ActiveTraces.getLocal().add("before filter", channel, latest);
        if (latest.isPresent()) {
            SortedSet<ContentKey> filtered = ContentKeyUtil.filter(latest
                    .map(Collections::singleton)
                    .orElseGet(Collections::emptySet), query);
            if (filtered.isEmpty()) {
                return Optional.empty();
            }
        }
        return latest;
    }

    public Optional<ContentKey> getLatest(String channel, boolean stable) {
        channel = getDisplayName(channel);
        final DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .next(false)
                .stable(stable)
                .count(1)
                .build();
        return getLatest(query);
    }

    private DirectionQuery configureStable(DirectionQuery query) {
        final ContentPath lastUpdated = getLatestLimit(query.getChannelName(), query.isStable());
        return query.withChannelStable(lastUpdated.getTime());
    }

    private ContentKey getLatestLimit(String channelName, boolean stable) {
        DateTime time = TimeUtil.now().plusMinutes(1);
        if (stable || !isLiveChannel(channelName)) {
            time = getLastUpdated(channelName, new ContentKey(TimeUtil.stable())).getTime();
        }
        return ContentKey.lastKey(time);
    }

    public void deleteBefore(String channel, ContentKey limitKey) {
        channel = getDisplayName(channel);
        contentService.deleteBefore(channel, limitKey);
    }

    public Optional<Content> get(ItemRequest itemRequest) {
        itemRequest = itemRequest.withChannel(getDisplayName(itemRequest.getChannel()));
        final DateTime limitTime = getChannelLimitTime(itemRequest.getChannel()).minusMinutes(15);
        if (itemRequest.getKey().getTime().isBefore(limitTime)) {
            return Optional.empty();
        }
        return contentService.get(itemRequest.getChannel(), itemRequest.getKey(), itemRequest.isRemoteOnly());
    }

    public Optional<ChannelConfig> getChannelConfig(String channelName, boolean allowChannelCache) {
        if (allowChannelCache) {
            return getCachedChannelConfig(channelName);
        }
        return Optional.ofNullable(channelConfigDao.get(channelName));
    }

    public Optional<ChannelConfig> getCachedChannelConfig(String channelName) {
        final ChannelConfig channelConfig = channelConfigDao.getCached(channelName);
        return Optional.ofNullable(channelConfig);
    }

    public boolean isLiveChannel(String channelName) {
        return getChannelConfig(channelName, true)
                .filter(ChannelConfig::isLive)
                .isPresent();
    }

    @SneakyThrows
    private ChannelConfig getExpectedCachedChannelConfig(String channelName) throws NoSuchChannelException {
        return getCachedChannelConfig(channelName)
                .orElseThrow(() -> {
                    throw new NoSuchChannelException(channelName);
                });
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

    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        if (query == null) {
            return Collections.emptySortedSet();
        }
        final String channelName = query.getChannelName();
        final ChannelConfig channelConfig = getCachedChannelConfig(channelName)
                .orElse(null);
        query = query.withChannelName(getDisplayName(channelName));
        query = query.withChannelConfig(channelConfig);
        final ContentPath lastUpdated = getLastUpdated(query.getChannelName(), new ContentKey(TimeUtil.time(query.isStable())));
        query = query.withChannelStable(lastUpdated.getTime());
        Stream<ContentKey> stream = contentService.queryByTime(query).stream();
        stream = ContentKeyUtil.enforceLimits(query, stream);
        return stream.collect(Collectors.toCollection(TreeSet::new));
    }

    public SortedSet<ContentKey> query(DirectionQuery query) {
        if (query.getCount() <= 0) {
            return Collections.emptySortedSet();
        }
        query = query.withChannelName(getDisplayName(query.getChannelName()));
        query = configureQuery(query);
        List<ContentKey> keys = new ArrayList<>(contentService.queryDirection(query));

        final SortedSet<ContentKey> contentKeys = ContentKeyUtil.filter(keys, query);
        if (query.isInclusive()) {
            if (!contentKeys.isEmpty()) {
                if (query.isNext()) {
                    contentKeys.remove(contentKeys.last());
                } else {
                    contentKeys.remove(contentKeys.first());
                }
            }
            contentKeys.add(query.getStartKey());
        }
        ActiveTraces.getLocal().add("ChannelService.query", contentKeys);
        return contentKeys;
    }

    private DirectionQuery configureQuery(DirectionQuery query) {
        ActiveTraces.getLocal().add("configureQuery.start", query);
        if (query.getCount() > contentProperties.getDirectionCountLimit()) {
            query = query.withCount(contentProperties.getDirectionCountLimit());
        }
        final String channelName = query.getChannelName();
        final ChannelConfig channelConfig = getExpectedCachedChannelConfig(channelName);
        query = query.withChannelConfig(channelConfig);
        final DateTime ttlTime = getChannelTtl(channelConfig, query.getEpoch());
        query = query.withEarliestTime(ttlTime);

        if (query.getStartKey() == null || query.getStartKey().getTime().isBefore(ttlTime)) {
            query = query.withStartKey(new ContentKey(ttlTime, "0"));
        }
        if (query.getEpoch().equals(Epoch.MUTABLE)) {
            if (!query.isNext()) {
                DateTime mutableTime = channelConfig.getMutableTime();
                if (query.getStartKey() == null || query.getStartKey().getTime().isAfter(mutableTime)) {
                    query = query.withStartKey(new ContentKey(mutableTime.plusMillis(1), "0"));
                }
            }
        }
        query = configureStable(query);
        ActiveTraces.getLocal().add("configureQuery.end", query);
        return query;
    }

    private DateTime getChannelTtl(ChannelConfig channelConfig, Epoch epoch) {
        DateTime ttlTime = channelConfig.getTtlTime();
        if (channelConfig.isHistorical()) {
            if (epoch.equals(Epoch.IMMUTABLE)) {
                ttlTime = channelConfig.getMutableTime().plusMillis(1);
            } else {
                final ContentKey lastKey = ContentKey.lastKey(channelConfig.getMutableTime());
                return lastContentPath.get(channelConfig.getDisplayName(), lastKey, HISTORICAL_EARLIEST).getTime();
            }
        }
        return ttlTime;
    }

    public void get(StreamResults streamResults) {
        streamResults = streamResults.withChannel(getDisplayName(streamResults.getChannel()));
        contentService.get(streamResults);
    }

    private DateTime getChannelLimitTime(String channelName) {
        final ChannelConfig channelConfig = getExpectedCachedChannelConfig(channelName);
        if (channelConfig.isHistorical()) {
            return TimeUtil.BIG_BANG;
        }
        return channelConfig.getTtlTime();
    }

    public boolean delete(String channelName) {
        channelName = getDisplayName(channelName);
        final Optional<ChannelConfig> optionalChannelConfig = getCachedChannelConfig(channelName);
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
        channelName = getDisplayName(channelName);
        final ChannelConfig channelConfig = getExpectedCachedChannelConfig(channelName);
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

    public ContentPath getLastUpdated(String channelName, ContentPath defaultValue) {
        channelName = getDisplayName(channelName);
        if (isReplicating(channelName)) {
            ContentPath contentPath = lastContentPath.get(channelName, defaultValue, REPLICATED_LAST_UPDATED);
            //REPLICATED_LAST_UPDATED is inclusive, and we want to be exclusive.
            if (!contentPath.equals(defaultValue)) {
                contentPath = new SecondPath(contentPath.getTime().plusSeconds(1));
            }
            return contentPath;
        }
        return defaultValue;
    }

    private String getDisplayName(String channelName) {
        final ChannelConfig channelConfig = getExpectedCachedChannelConfig(channelName);
        return channelConfig.getDisplayName();
    }
}
