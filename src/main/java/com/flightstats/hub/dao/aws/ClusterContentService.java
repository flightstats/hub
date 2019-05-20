package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.ClusterStateDao;
import com.flightstats.hub.cluster.LatestContentCache;
import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.config.ContentProperties;
import com.flightstats.hub.config.SpokeProperties;
import com.flightstats.hub.dao.ChannelService;
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
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public class ClusterContentService implements ContentService {

    private static final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ClusterContentService-%d").build());

    private final ContentDao spokeWriteContentDao;
    private final ContentDao s3SingleContentDao;
    private final ContentDao spokeReadContentDao;
    private final ContentDao s3BatchContentDao;
    private final ContentDao s3LargePayloadContentDao;
    private final LatestContentCache latestContentCache;
    private final WriteQueue writeQueue;
    private final ChannelService channelService;
    private final ClusterStateDao clusterStateDao;
    private final HubUtils hubUtils;
    private final LargeContentUtils largeContentUtils;
    private final AppProperties appProperties;
    private final ContentProperties contentProperties;
    private final SpokeProperties spokeProperties;
    private ScheduledExecutorService zkCacheStateUpdateExecutor;

    @Inject
    public ClusterContentService(
                            ChannelService channelService,
                            @Named(ContentDao.WRITE_CACHE) ContentDao spokeWriteContentDao,
                            @Named(ContentDao.READ_CACHE) ContentDao spokeReadContentDao,
                            @Named(ContentDao.SINGLE_LONG_TERM) ContentDao s3SingleContentDao,
                            @Named(ContentDao.LARGE_PAYLOAD) ContentDao s3LargePayloadContentDao,
                            @Named(ContentDao.BATCH_LONG_TERM) ContentDao s3BatchContentDao,
                            LatestContentCache latestContentCache,
                            WriteQueue writeQueue,
                            ClusterStateDao clusterStateDao,
                            HubUtils hubUtils,
                            LargeContentUtils largeContentUtils,
                            AppProperties appProperties,
                            ContentProperties contentProperties,
                            SpokeProperties spokeProperties) {
        HubServices.registerPreStop(new SpokeS3ContentServiceInit());
        this.channelService = channelService;
        this.spokeWriteContentDao = spokeWriteContentDao;
        this.spokeReadContentDao = spokeReadContentDao;
        this.s3SingleContentDao = s3SingleContentDao;
        this.s3LargePayloadContentDao = s3LargePayloadContentDao;
        this.s3BatchContentDao = s3BatchContentDao;
        this.latestContentCache = latestContentCache;
        this.writeQueue = writeQueue;
        this.clusterStateDao = clusterStateDao;
        this.hubUtils = hubUtils;
        this.largeContentUtils = largeContentUtils;
        this.appProperties = appProperties;
        this.contentProperties = contentProperties;
        this.spokeProperties = spokeProperties;
        this.zkCacheStateUpdateExecutor = Executors.newScheduledThreadPool(4);
    }

    private SortedSet<ContentKey> query(Function<ContentDao, SortedSet<ContentKey>> daoQuery, List<ContentDao> contentDaos) {
        try {
            QueryResult queryResult = new QueryResult(contentDaos.size());
            CountDownLatch latch = new CountDownLatch(contentDaos.size());
            Traces traces = ActiveTraces.getLocal();
            String threadName = Thread.currentThread().getName();
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
    public ContentKey insert(String channelName, Content content) {
        final Content spokeContent = adjustContentIfLarge(channelName, content);

        // after stable() seconds, we want to write the cache entry if the write succeeded
        SortedSet<ContentKey> keys = setStableCache(channelName,
                () -> new TreeSet<>(
                        Arrays.asList(spokeWriteContentDao.insert(channelName, spokeContent))));

        if (isWriteable(channelName)) {
            s3SingleWrite(channelName, keys.first());
        }
        return keys.first();
    }

    private Content adjustContentIfLarge(String channelName, Content content) {
        Content spokeContent = content;
        if (content.isLarge()) {
            s3LargePayloadContentDao.insert(channelName, content);
            spokeContent = largeContentUtils.createIndex(content);
        }
        return spokeContent;
    }

    private SortedSet<ContentKey> setStableCache(String channelName, Supplier<SortedSet<ContentKey>> supplier) {
        // after stable() seconds, we want to write the cache entry if the write succeeded
        AtomicReference<ContentKey> ref = new AtomicReference<>();

        ScheduledFuture future = zkCacheStateUpdateExecutor.schedule(() -> {
            if (ref.get() != null) {
                latestContentCache.setIfAfter(channelName, ref.get());
            }
        }, contentProperties.getStableSeconds() +1, TimeUnit.SECONDS);

        SortedSet<ContentKey> keys;
        try {
            keys = supplier.get();
            ref.set(keys.last());
        } catch(Exception e) {
            future.cancel(true);
            throw e;
        }
        return keys;
    }

    private void s3SingleWrite(String channelName, ContentKey key) {
        writeQueue.add(new ChannelContentKey(channelName, key));
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) {
        String channelName = bulkContent.getChannel();

        return setStableCache(channelName, () -> {
            // set stable cache in ZK here as well
            SortedSet<ContentKey> keys = spokeWriteContentDao.insert(bulkContent);
            if (isWriteable(channelName)) {
                for (ContentKey key : keys) {
                    s3SingleWrite(channelName, key);
                }
            }
            return keys;
        });
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
        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(channelName);
        if (!optionalChannelConfig.isPresent()) return Optional.empty();
        ChannelConfig channelConfig = optionalChannelConfig.get();
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
            Map<ContentKey, Content> map = s3BatchContentDao.readBatch(channelName, key);
            Content content = map.get(key);
            if (content == null) {
                return null;
            }
            Content copy = Content.copy(content);
            storeBatchInReadCache(channelName, map);
            return copy;
        } catch (IOException e) {
            log.warn("unable to get batch from long term storage", e);
            return null;
        }
    }

    private void storeBatchInReadCache(String channelName, Map<ContentKey, Content> map) {
        try {
            BulkContent bulkContent = BulkContent.fromMap(channelName, map);
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
            ContentKey indexKey = content.getContentKey().get();
            Content largeMeta = largeContentUtils.fromIndex(content);
            content = s3LargePayloadContentDao.get(channelName, largeMeta.getContentKey().get());
            content.setContentKey(indexKey);
            content.setSize(largeMeta.getSize());
        }
        return Optional.of(content);
    }

    private DateTime getSpokeTtlTime(String channelName) {
        DateTime startTime = channelService.adjustLastUpdatePathIfReplicating(channelName, new ContentKey(TimeUtil.now())).getTime();
        return startTime.minusMinutes(spokeProperties.getTtlMinutes(SpokeStore.WRITE));
    }

    @Override
    public void get(StreamResults streamResults) {
        String channelName = streamResults.getChannel();
        Consumer<Content> callback = streamResults.getCallback();
        List<MinutePath> minutePaths = new ArrayList<>(ContentKeyUtil.convert(streamResults.getKeys()));
        if (streamResults.isDescending()) {
            Collections.reverse(minutePaths);
        }
        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(channelName);
        boolean isSingleChannel = optionalChannelConfig.isPresent() &&
                optionalChannelConfig.get().isSingle();
        DateTime spokeTtlTime = getSpokeTtlTime(channelName);
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
        List<ContentKey> keys = new ArrayList<>(contentPathKeys.getKeys());
        if (descending) {
            Collections.reverse(keys);
        }
        for (ContentKey contentKey : keys) {
            Optional<Content> contentOptional = get(channelName, contentKey, false);
            if (contentOptional.isPresent()) {
                callback.accept(contentOptional.get());
            }
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
        List<ContentDao> daos = new ArrayList<>();
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
//            ChannelConfig channel = channelService.getCachedChannelConfig(query.getChannelName());
            Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(query.getChannelName());
            if (!optionalChannelConfig.isPresent()) return new TreeSet<>();
            ChannelConfig channelConfig = optionalChannelConfig.get();
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
        switch (query.getEpoch()) {
            case IMMUTABLE:
                return getLatestImmutable(query);
            case MUTABLE:
                return ContentService.chooseLatest(queryDirection(query));
            default:
                Optional<ContentKey> latestImmutable = getLatestImmutable(query);
                if (latestImmutable.isPresent()) {
                    return latestImmutable;
                }
                return ContentService.chooseLatest(queryDirection(query));
        }
    }

    private Optional<ContentKey> getLatestImmutable(DirectionQuery latestQuery) {
        String channel = latestQuery.getChannelName();
        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(channel);
        if (!channelService.getCachedChannelConfig(channel).isPresent()) {
            return Optional.empty();
        }

        DateTime channelTtlTime = optionalChannelConfig.get().getTtlTime();
        Optional<ContentKey> cachedKey = getLatestCachedKeyIfNotExpired(channel, channelTtlTime);
        if (latestQuery.isStable() && cachedKey.isPresent()) {
            return cachedKey;
        }

        return findLatestKey(latestQuery, channel, cachedKey);
    }

    private Optional<ContentKey> getLatestCachedKeyIfNotExpired(String channel, DateTime channelTtlTime) {
        ContentPath latestCache = latestContentCache.getLatest(channel, null);
        ActiveTraces.getLocal().add("found latestCache", channel, latestCache);
        if (latestCache != null) {
            // TODO: Inconsistent read.  This uses a value it considers stale, but guarantees subsequent reads will not use it.
            if (latestCache.getTime().isBefore(channelTtlTime)) {
                latestContentCache.setEmpty(channel);   // if the newest thing (latest) is expired, then the channel is now empty
            }
            ActiveTraces.getLocal().add("found cached latest", channel, latestCache);
            if (latestCache.equals(ContentKey.NONE)) {
                return Optional.empty();
            }
        }
        return Optional.ofNullable((ContentKey) latestCache);
    }

    private Optional<ContentKey> findLatestKey(DirectionQuery latestQuery, String channel, Optional<ContentKey> cachedKey) {
        Optional<ContentKey> latest = spokeWriteContentDao.getLatest(channel, latestQuery.getStartKey(), ActiveTraces.getLocal());
        boolean shouldUpdateCache = latestQuery.isStable();

        if (!latest.isPresent()) {
            if (cachedKey.isPresent()) {
                latest = cachedKey;
            } else {
                latest = findLatestFromAllSources(latestQuery, channel);
                shouldUpdateCache = true;
            }
        }

        if (shouldUpdateCache) {
            if (latest.isPresent()) {
                ActiveTraces.getLocal().add("updating cache with latestKey {} {}", channel, latest.get());
                latestContentCache.setIfAfter(channel, latest.get());
            } else {
                ActiveTraces.getLocal().add("updating channel empty", channel);
                latestContentCache.setIfAfter(channel, ContentKey.NONE);
            }
        }
        return latest;
    }

    private Optional<ContentKey> findLatestFromAllSources(DirectionQuery latestQuery, String channel) {
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .startKey(latestQuery.getStartKey())
                .next(false)
                .stable(latestQuery.isStable())
                .epoch(latestQuery.getEpoch())
                .location(latestQuery.getLocation())
                .count(1)
                .build();
        SortedSet<ContentKey> keys = channelService.query(query);
        return keys.isEmpty() ? Optional.empty() : Optional.of(keys.last());
    }

    @Override
    public void delete(String channelName) {
        log.info("deleting channel " + channelName);
        spokeWriteContentDao.delete(channelName);
        spokeReadContentDao.delete(channelName);
        s3SingleContentDao.delete(channelName);
        s3BatchContentDao.delete(channelName);
        s3LargePayloadContentDao.delete(channelName);
        latestContentCache.deleteCache(channelName);
        clusterStateDao.delete(channelName, S3Verifier.LAST_SINGLE_VERIFIED);
        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(channelName);
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
            latestContentCache.setEmpty(newConfig.getDisplayName());
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
                updateLastEntryPointerToNewlyImmutableEntry(newConfig, oldConfig);
            }
        }
    }

    private void updateLastEntryPointerToNewlyImmutableEntry(ChannelConfig newConfig, ChannelConfig oldConfig) {
        ContentPath latestOrNoneIfEmpty = latestContentCache.getLatest(newConfig.getDisplayName(), ContentKey.NONE);
        log.info("handleMutableTimeChange {}", latestOrNoneIfEmpty);
        if (latestOrNoneIfEmpty.equals(ContentKey.NONE)) {
            DirectionQuery query = DirectionQuery.builder()
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
            Optional<ContentKey> mutableLatest = getLatest(query);
            ActiveTraces.getLocal().log(log);
            if (mutableLatest.isPresent()) {
                ContentKey mutableKey = mutableLatest.get();
                if (mutableKey.getTime().isAfter(newConfig.getMutableTime())) {
                    log.info("handleMutableTimeChange.updateIncrease {}", mutableKey);
                    latestContentCache.setIfAfter(newConfig.getDisplayName(), mutableKey);
                }
            }
        }
    }

    private boolean isWriteable(String channelName) {
        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(channelName);
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

}
