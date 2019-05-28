package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.ContentProperties;
import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.dao.aws.writeQueue.WriteQueue;
import com.flightstats.hub.exception.FailedQueryException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.ContentPathKeys;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.LargeContentUtils;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.Query;
import com.flightstats.hub.model.StreamResults;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.flightstats.hub.constant.ZookeeperNodes.LAST_SINGLE_VERIFIED;

@Slf4j
public class ClusterContentService implements ContentService {

    private static final String CHANNEL_LATEST_UPDATED = "/ChannelLatestUpdated/";
    private static final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ClusterContentService-%d").build());

    private final ContentDao spokeWriteContentDao;
    private final ContentDao s3SingleContentDao;
    private final ContentDao spokeReadContentDao;
    private final ContentDao s3BatchContentDao;
    private final ContentDao s3LargePayloadContentDao;
    private final WriteQueue writeQueue;
    private final ContentRetriever contentRetriever;
    private final LastContentPath lastContentPath;
    private final HubUtils hubUtils;
    private final LargeContentUtils largeContentUtils;
    private final AppProperties appProperties;
    private final ContentProperties contentProperties;
    private final SpokeProperties spokeProperties;

    @Inject
    public ClusterContentService(
            @Named(ContentDao.WRITE_CACHE) ContentDao spokeWriteContentDao,
            @Named(ContentDao.READ_CACHE) ContentDao spokeReadContentDao,
            @Named(ContentDao.SINGLE_LONG_TERM) ContentDao s3SingleContentDao,
            @Named(ContentDao.LARGE_PAYLOAD) ContentDao s3LargePayloadContentDao,
            @Named(ContentDao.BATCH_LONG_TERM) ContentDao s3BatchContentDao,
            WriteQueue writeQueue,
            ContentRetriever contentRetriever,
            LastContentPath lastContentPath,
            HubUtils hubUtils,
            LargeContentUtils largeContentUtils,
            AppProperties appProperties,
            ContentProperties contentProperties,
            SpokeProperties spokeProperties) {
        HubServices.registerPreStop(new SpokeS3ContentServiceInit());
        if (contentProperties.isChannelProtectionEnabled() || contentProperties.isLatestUpdateServiceEnabled()) {
            HubServices.register(new ChannelLatestUpdatedService(contentProperties), HubServices.TYPE.AFTER_HEALTHY_START);
        }
        this.spokeWriteContentDao = spokeWriteContentDao;
        this.spokeReadContentDao = spokeReadContentDao;
        this.s3SingleContentDao = s3SingleContentDao;
        this.s3LargePayloadContentDao = s3LargePayloadContentDao;
        this.s3BatchContentDao = s3BatchContentDao;
        this.writeQueue = writeQueue;
        this.lastContentPath = lastContentPath;
        this.hubUtils = hubUtils;
        this.largeContentUtils = largeContentUtils;
        this.contentRetriever = contentRetriever;
        this.appProperties = appProperties;
        this.contentProperties = contentProperties;
        this.spokeProperties = spokeProperties;
    }

    private SortedSet<ContentKey> query(Function<ContentDao, SortedSet<ContentKey>> daoQuery, List<ContentDao> contentDaos) {
        try {
            final QueryResult queryResult = new QueryResult(contentDaos.size());
            final CountDownLatch latch = new CountDownLatch(contentDaos.size());
            final Traces traces = ActiveTraces.getLocal();
            final String threadName = Thread.currentThread().getName();
            for (ContentDao contentDao : contentDaos) {
                executorService.submit(() -> {
                    Thread.currentThread().setName(contentDao.getClass().getSimpleName() + "|" + threadName);
                    ActiveTraces.setLocal(traces);
                    try {
                        queryResult.addKeys(daoQuery.apply(contentDao));
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await(contentProperties.getQueryMergeMaxWaitInMins(), TimeUnit.MINUTES);
            if (queryResult.hadSuccess()) {
                return queryResult.getContentKeys();
            } else {
                traces.add("unable to complete query ", queryResult);
                throw new FailedQueryException("unable to complete query " + queryResult + " " + threadName);
            }
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        Content spokeContent = content;
        if (content.isLarge()) {
            s3LargePayloadContentDao.insert(channelName, content);
            spokeContent = largeContentUtils.createIndex(content);
        }
        final ContentKey key = spokeWriteContentDao.insert(channelName, spokeContent);
        if (isWriteable(channelName)) {
            s3SingleWrite(channelName, key);
        }
        return key;
    }

    private void s3SingleWrite(String channelName, ContentKey key) {
        writeQueue.add(new ChannelContentKey(channelName, key));
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        final String channelName = bulkContent.getChannel();
        final SortedSet<ContentKey> keys = spokeWriteContentDao.insert(bulkContent);
        if (isWriteable(channelName)) {
            for (ContentKey key : keys) {
                s3SingleWrite(channelName, key);
            }
        }
        return keys;
    }

    @Override
    public boolean historicalInsert(String channelName, Content content) throws Exception {
        if (content.isLarge()) {
            s3LargePayloadContentDao.insertHistorical(channelName, content);
        } else {
            s3SingleContentDao.insertHistorical(channelName, content);
        }
        return true;
    }

    @Override
    public Optional<Content> get(String channelName, ContentKey key, boolean remoteOnly) {
        log.trace("fetching {} from channel {} ", key.toString(), channelName);
        final Optional<ChannelConfig> optionalChannelConfig = contentRetriever.getCachedChannelConfig(channelName);
        if (!optionalChannelConfig.isPresent()) return Optional.empty();
        final ChannelConfig channelConfig = optionalChannelConfig.get();
        if (!remoteOnly && key.getTime().isAfter(getSpokeTtlTime(channelName))) {
            Content content = spokeWriteContentDao.get(channelName, key);
            if (content != null) {
                log.trace("returning from spoke {} {}", key.toString(), channelName);
                return checkForLargeIndex(channelName, content);
            }
        }
        Content content;
        if (channelConfig.isSingle()) {
            content = s3SingleContentDao.get(channelName, key);
        } else if (channelConfig.isBatch()) {
            content = spokeReadContentDao.get(channelName, key);
            if (content == null) {
                content = getFromS3BatchAndStoreInReadCache(channelName, key);
            }
        } else {
            content = spokeReadContentDao.get(channelName, key);
            if (content == null) {
                content = getFromS3BatchAndStoreInReadCache(channelName, key);
            }
            if (content == null) {
                content = s3SingleContentDao.get(channelName, key);
            }
        }
        return checkForLargeIndex(channelName, content);
    }

    private Content getFromS3BatchAndStoreInReadCache(String channelName, ContentKey key) {
        try {
            final Map<ContentKey, Content> map = s3BatchContentDao.readBatch(channelName, key);
            final Content content = map.get(key);
            if (content == null) {
                return null;
            }
            final Content copy = Content.copy(content);
            storeBatchInReadCache(channelName, map);
            return copy;
        } catch (IOException e) {
            log.warn("unable to get batch from long term storage", e);
            return null;
        }
    }

    private void storeBatchInReadCache(String channelName, Map<ContentKey, Content> map) {
        try {
            final BulkContent bulkContent = BulkContent.fromMap(channelName, map);
            spokeReadContentDao.insert(bulkContent);
        } catch (Exception e) {
            log.warn("unable to cache batch", e);
        }
    }

    private Optional<Content> checkForLargeIndex(String channelName, Content content) {
        if (content == null) {
            return Optional.empty();
        }
        if (content.isIndexForLarge()) {
            final ContentKey indexKey = content.getContentKey().get();
            final Content largeMeta = largeContentUtils.fromIndex(content);
            content = s3LargePayloadContentDao.get(channelName, largeMeta.getContentKey().get());
            content.setContentKey(indexKey);
            content.setSize(largeMeta.getSize());
        }
        return Optional.of(content);
    }

    private DateTime getSpokeTtlTime(String channelName) {
        DateTime startTime = contentRetriever.getLastUpdated(channelName, new ContentKey(TimeUtil.now())).getTime();
        return startTime.minusMinutes(spokeProperties.getTtlMinutes(SpokeStore.WRITE));
    }

    @Override
    public void get(StreamResults streamResults) {
        final String channelName = streamResults.getChannel();
        final Consumer<Content> callback = streamResults.getCallback();
        final List<MinutePath> minutePaths = new ArrayList<>(ContentKeyUtil.convert(streamResults.getKeys()));
        if (streamResults.isDescending()) {
            Collections.reverse(minutePaths);
        }
        final Optional<ChannelConfig> optionalChannelConfig = contentRetriever.getCachedChannelConfig(channelName);
        final boolean isSingleChannel = optionalChannelConfig.isPresent() &&
                optionalChannelConfig.get().isSingle();
        final DateTime spokeTtlTime = getSpokeTtlTime(channelName);
        for (MinutePath minutePath : minutePaths) {
            if (minutePath.getTime().isAfter(spokeTtlTime) || isSingleChannel) {
                getValues(channelName, streamResults.getCallback(), minutePath, streamResults.isDescending());
            } else {
                if (!s3BatchContentDao.streamMinute(channelName, minutePath, streamResults.isDescending(), callback)) {
                    getValues(channelName, callback, minutePath, streamResults.isDescending());
                }
            }
        }
    }

    private void getValues(String channelName, Consumer<Content> callback, ContentPathKeys contentPathKeys, boolean descending) {
        final List<ContentKey> keys = new ArrayList<>(contentPathKeys.getKeys());
        if (descending) {
            Collections.reverse(keys);
        }
        for (ContentKey contentKey : keys) {
            final Optional<Content> contentOptional = get(channelName, contentKey, false);
            contentOptional.ifPresent(callback::accept);
        }
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery query) {
        return handleQuery(query, contentDao -> contentDao.queryByTime(query));
    }

    @Override
    public Collection<ContentKey> queryDirection(DirectionQuery query) {
        return handleQuery(query, contentDao -> contentDao.query(query));
    }

    private Collection<ContentKey> handleQuery(Query query, Function<ContentDao, SortedSet<ContentKey>> daoQuery) {
        final List<ContentDao> daos = new ArrayList<>();
        if (query.getLocation().equals(Location.CACHE)) {
            daos.add(spokeWriteContentDao);
            daos.add(spokeReadContentDao);
        } else if (query.getLocation().equals(Location.CACHE_WRITE)) {
            daos.add(spokeWriteContentDao);
        } else if (query.getLocation().equals(Location.CACHE_READ)) {
            daos.add(spokeReadContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM)) {
            daos.add(s3SingleContentDao);
            daos.add(s3BatchContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM_SINGLE)) {
            daos.add(s3SingleContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM_BATCH)) {
            daos.add(s3BatchContentDao);
        } else {
            daos.add(spokeWriteContentDao);
            final Optional<ChannelConfig> optionalChannelConfig = contentRetriever.getCachedChannelConfig(query.getChannelName());
            if (!optionalChannelConfig.isPresent()) return new TreeSet<>();
            final ChannelConfig channelConfig = optionalChannelConfig.get();
            DateTime spokeTtlTime = getSpokeTtlTime(query.getChannelName());
            if (channelConfig.isHistorical() && channelConfig.getMutableTime().isAfter(spokeTtlTime)) {
                spokeTtlTime = channelConfig.getMutableTime();
            }
            if (query.outsideOfCache(spokeTtlTime)) {
                if (channelConfig.isSingle()) {
                    daos.add(s3SingleContentDao);
                } else if (channelConfig.isBatch()) {
                    daos.add(s3BatchContentDao);
                } else {
                    daos.add(s3SingleContentDao);
                    daos.add(s3BatchContentDao);
                }
            }
        }
        return query(daoQuery, daos);
    }

    @Override
    public Optional<ContentKey> getLatest(DirectionQuery query) {
        if (query.getEpoch().equals(Epoch.IMMUTABLE)) {
            return getLatestImmutable(query);
        } else if (query.getEpoch().equals(Epoch.MUTABLE)) {
            return ContentService.chooseLatest(queryDirection(query), query);
        } else {
            Optional<ContentKey> latestImmutable = getLatestImmutable(query);
            if (latestImmutable.isPresent()) {
                return latestImmutable;
            }
            return ContentService.chooseLatest(queryDirection(query), query);
        }
    }

    private Optional<ContentKey> getLatestImmutable(DirectionQuery latestQuery) {
        final String channel = latestQuery.getChannelName();
        final DateTime cacheTtlTime = getSpokeTtlTime(channel);
        final Optional<ContentKey> latest = spokeWriteContentDao.getLatest(channel, latestQuery.getStartKey(), ActiveTraces.getLocal());

        final Optional<ChannelConfig> optionalChannelConfig = contentRetriever.getCachedChannelConfig(channel);
        if (!optionalChannelConfig.isPresent()) return Optional.empty();
        final ChannelConfig cachedChannelConfig = optionalChannelConfig.get();

        if (latest.isPresent()) {
            ActiveTraces.getLocal().add("found spoke latest", channel, latest);
            lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            return latest;
        }
        final ContentPath latestCache = lastContentPath.get(channel, null, CHANNEL_LATEST_UPDATED);
        ActiveTraces.getLocal().add("found latestCache", channel, latestCache);
        if (latestCache != null) {
            DateTime channelTtlTime = cachedChannelConfig.getTtlTime();
            if (latestCache.getTime().isBefore(channelTtlTime)) {
                lastContentPath.update(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            }
            ActiveTraces.getLocal().add("found cached latest", channel, latest);
            if (latestCache.equals(ContentKey.NONE)) {
                return Optional.empty();
            }
            return Optional.of((ContentKey) latestCache);
        }
        final DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .startKey(latestQuery.getStartKey())
                .next(false)
                .stable(latestQuery.isStable())
                .epoch(latestQuery.getEpoch())
                .location(latestQuery.getLocation())
                .count(1)
                .build();
        final Collection<ContentKey> keys = contentRetriever.query(query);
        if (keys.isEmpty()) {
            ActiveTraces.getLocal().add("updating channel empty", channel);
            lastContentPath.updateIncrease(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            return Optional.empty();
        } else {
            final ContentKey latestKey = keys.iterator().next();
            if (latestKey.getTime().isAfter(cacheTtlTime)) {
                ActiveTraces.getLocal().add("latestKey within spoke window {} {}", channel, latestKey);
                lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            } else {
                ActiveTraces.getLocal().add("updating cache with latestKey {} {}", channel, latestKey);
                lastContentPath.update(latestKey, channel, CHANNEL_LATEST_UPDATED);
            }
            return Optional.of(latestKey);
        }
    }

    @Override
    public void delete(String channelName) {
        log.info("deleting channel " + channelName);
        spokeWriteContentDao.delete(channelName);
        spokeReadContentDao.delete(channelName);
        s3SingleContentDao.delete(channelName);
        s3BatchContentDao.delete(channelName);
        s3LargePayloadContentDao.delete(channelName);
        lastContentPath.delete(channelName, CHANNEL_LATEST_UPDATED);
        lastContentPath.delete(channelName, LAST_SINGLE_VERIFIED);
        final Optional<ChannelConfig> optionalChannelConfig = contentRetriever.getCachedChannelConfig(channelName);
        if (optionalChannelConfig.isPresent() && !optionalChannelConfig.get().isSingle()) {
            new S3Batch(optionalChannelConfig.get(), hubUtils, appProperties.getAppUrl(), appProperties.getAppEnv()).stop();
        }
    }

    @Override
    public void delete(String channelName, ContentKey contentKey) {
        s3SingleContentDao.delete(channelName, contentKey);
        s3LargePayloadContentDao.delete(channelName, contentKey);
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        s3SingleContentDao.deleteBefore(name, limitKey);
        s3BatchContentDao.deleteBefore(name, limitKey);
        s3LargePayloadContentDao.deleteBefore(name, limitKey);
    }

    @Override
    public void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        if (oldConfig == null) {
            lastContentPath.updateIncrease(ContentKey.NONE, newConfig.getDisplayName(), CHANNEL_LATEST_UPDATED);
        }

        final S3Batch s3Batch = new S3Batch(
                newConfig,
                hubUtils,
                appProperties.getAppUrl(),
                appProperties.getAppEnv());

        if (newConfig.isSingle()) {
            if (oldConfig != null && !oldConfig.isSingle()) {
                s3Batch.stop();
            }
        } else {
            s3Batch.start();
        }
        if (newConfig.isHistorical() && oldConfig != null && oldConfig.isHistorical()) {
            if (newConfig.getMutableTime().isBefore(oldConfig.getMutableTime())) {
                handleMutableTimeChange(newConfig, oldConfig);
            }
        }
    }

    private void handleMutableTimeChange(ChannelConfig newConfig, ChannelConfig oldConfig) {
        final ContentPath latest = lastContentPath.get(newConfig.getDisplayName(), ContentKey.NONE, CHANNEL_LATEST_UPDATED);
        log.info("handleMutableTimeChange {}", latest);
        if (latest.equals(ContentKey.NONE)) {
            final DirectionQuery query = DirectionQuery.builder()
                    .startKey(ContentKey.lastKey(oldConfig.getMutableTime().plusMillis(1)))
                    .earliestTime(newConfig.getMutableTime())
                    .channelName(newConfig.getDisplayName())
                    .channelConfig(oldConfig)
                    .next(false)
                    .stable(true)
                    .epoch(Epoch.MUTABLE)
                    .location(Location.LONG_TERM_SINGLE)
                    .count(1)
                    .build();
            final Optional<ContentKey> mutableLatest = getLatest(query);
            ActiveTraces.getLocal().log(log);
            if (mutableLatest.isPresent()) {
                ContentKey mutableKey = mutableLatest.get();
                if (mutableKey.getTime().isAfter(newConfig.getMutableTime())) {
                    log.info("handleMutableTimeChange.updateIncrease {}", mutableKey);
                    lastContentPath.updateIncrease(mutableKey, newConfig.getDisplayName(), CHANNEL_LATEST_UPDATED);
                }
            }
        }
    }

    private boolean isWriteable(String channelName) {
        final Optional<ChannelConfig> optionalChannelConfig = contentRetriever.getCachedChannelConfig(channelName);
        return optionalChannelConfig.isPresent() && !optionalChannelConfig.get().isBatch();
    }

    private class SpokeS3ContentServiceInit extends AbstractIdleService {
        @Override
        protected void startUp() {
            spokeWriteContentDao.initialize();
            spokeReadContentDao.initialize();
            s3SingleContentDao.initialize();
            s3BatchContentDao.initialize();
        }

        @Override
        protected void shutDown() {
            //do nothing
        }
    }

    private class ChannelLatestUpdatedService extends AbstractScheduledService {

        private ContentProperties contentProperties;

        public ChannelLatestUpdatedService(ContentProperties contentProperties) {
            this.contentProperties = contentProperties;
        }

        @Override
        protected synchronized void runOneIteration() {
            log.debug("running...");
            ActiveTraces.start("ChannelLatestUpdatedService");
            contentRetriever.getChannelConfig().forEach(channelConfig -> {
                try {
                    DateTime time = TimeUtil.stable().plusMinutes(1);
                    Traces traces = new Traces(contentProperties, channelConfig.getDisplayName(), time);
                    DirectionQuery latestQuery = DirectionQuery.builder()
                            .channelName(channelConfig.getDisplayName())
                            .next(false)
                            .stable(false)
                            .location(Location.ALL)
                            .epoch(Epoch.IMMUTABLE)
                            .startKey(ContentKey.lastKey(time))
                            .count(1)
                            .build();
                    Optional<ContentKey> latest = getLatest(latestQuery);
                    log.debug("latest updated {} {}", channelConfig.getDisplayName(), latest);
                    traces.log(log);
                } catch (Exception e) {
                    log.warn("unexpected ChannelLatestUpdatedService issue " + channelConfig.getDisplayName(), e);
                }
            });
            ActiveTraces.end();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(2, 59, TimeUnit.MINUTES);
        }

    }

}
