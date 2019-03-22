package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.QueryResult;
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
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.function.Supplier;

import static com.flightstats.hub.model.LargeContent.createIndex;
import static com.flightstats.hub.model.LargeContent.fromIndex;

public class ClusterContentService implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(ClusterContentService.class);
    private static final String CHANNEL_LATEST_UPDATED = "/ChannelLatestUpdated/";
    private static final long largePayload = HubProperties.getLargePayload();
    private static final int queryMergeMaxWaitMinutes = HubProperties.getProperty("query.merge.max.wait.minutes", 2);
    private static final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ClusterContentService-%d").build());
    private final boolean dropSomeWrites = HubProperties.getProperty("s3.dropSomeWrites", false);
    @Inject
    @Named(ContentDao.WRITE_CACHE)
    private ContentDao spokeWriteContentDao;
    @Inject
    @Named(ContentDao.SINGLE_LONG_TERM)
    private ContentDao s3SingleContentDao;
    @Inject
    @Named(ContentDao.READ_CACHE)
    private ContentDao spokeReadContentDao;
    @Inject
    @Named(ContentDao.BATCH_LONG_TERM)
    private ContentDao s3BatchContentDao;
    @Inject
    @Named(ContentDao.LARGE_PAYLOAD)
    private ContentDao s3LargePayloadContentDao;
    @Inject
    private ChannelService channelService;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private S3WriteQueue s3WriteQueue;
    @Inject
    private HubUtils hubUtils;

    public ClusterContentService() {
        HubServices.registerPreStop(new SpokeS3ContentServiceInit());
        HubServices.register(new ChannelLatestUpdatedService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    private static SortedSet<ContentKey> query(Function<ContentDao, SortedSet<ContentKey>> daoQuery, List<ContentDao> contentDaos) {
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
            latch.await(queryMergeMaxWaitMinutes, TimeUnit.MINUTES);
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
            spokeContent = createIndex(content);
        }
        ContentKey key = spokeWriteContentDao.insert(channelName, spokeContent);
        if (isWriteable(channelName)) {
            Supplier<Void> local = () -> {
                s3SingleWrite(channelName, key, content.isForceWrite());
                return null;
            };
        }
        return key;
    }

    private void s3SingleWrite(String channelName, ContentKey key, boolean forceWrite) {
        if (!forceWrite && dropSomeWrites && Math.random() > 0.5) {
            logger.debug("dropping {} {}", channelName, key);
        } else {
            s3WriteQueue.add(new ChannelContentKey(channelName, key));
        }
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        String channelName = bulkContent.getChannel();
        SortedSet<ContentKey> keys = spokeWriteContentDao.insert(bulkContent);
        if (isWriteable(channelName)) {
            for (ContentKey key : keys) {
                s3SingleWrite(channelName, key, false);
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
        logger.trace("fetching {} from channel {} ", key.toString(), channelName);
        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(channelName);
        if (!optionalChannelConfig.isPresent()) return Optional.empty();
        ChannelConfig channelConfig = optionalChannelConfig.get();
        if (!remoteOnly && key.getTime().isAfter(getSpokeTtlTime(channelName))) {
            Content content = spokeWriteContentDao.get(channelName, key);
            if (content != null) {
                logger.trace("returning from spoke {} {}", key.toString(), channelName);
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
            logger.warn("unable to get batch from long term storage", e);
            return null;
        }
    }

    private void storeBatchInReadCache(String channelName, Map<ContentKey, Content> map) {
        try {
            BulkContent bulkContent = BulkContent.fromMap(channelName, map);
            spokeReadContentDao.insert(bulkContent);
        } catch (Exception e) {
            logger.warn("unable to cache batch", e);
        }
    }

    private Optional<Content> checkForLargeIndex(String channelName, Content content) {
        if (content == null) {
            return Optional.empty();
        }
        if (content.isIndexForLarge()) {
            ContentKey indexKey = content.getContentKey().get();
            Content largeMeta = fromIndex(content);
            content = s3LargePayloadContentDao.get(channelName, largeMeta.getContentKey().get());
            content.setContentKey(indexKey);
            content.setSize(largeMeta.getSize());
        }
        return Optional.of(content);
    }

    private DateTime getSpokeTtlTime(String channelName) {
        DateTime startTime = channelService.getLastUpdated(channelName, new ContentKey(TimeUtil.now())).getTime();
        return startTime.minusMinutes(HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE));
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
        String channel = latestQuery.getChannelName();
        DateTime cacheTtlTime = getSpokeTtlTime(channel);
        Optional<ContentKey> latest = spokeWriteContentDao.getLatest(channel, latestQuery.getStartKey(), ActiveTraces.getLocal());

        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(channel);
        if (!optionalChannelConfig.isPresent()) return Optional.empty();
        ChannelConfig cachedChannelConfig = optionalChannelConfig.get();

        if (latest.isPresent()) {
            ActiveTraces.getLocal().add("found spoke latest", channel, latest);
            lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            return latest;
        }
        ContentPath latestCache = lastContentPath.get(channel, null, CHANNEL_LATEST_UPDATED);
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
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .startKey(latestQuery.getStartKey())
                .next(false)
                .stable(latestQuery.isStable())
                .epoch(latestQuery.getEpoch())
                .location(latestQuery.getLocation())
                .count(1)
                .build();
        Collection<ContentKey> keys = channelService.query(query);
        if (keys.isEmpty()) {
            ActiveTraces.getLocal().add("updating channel empty", channel);
            lastContentPath.updateIncrease(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            return Optional.empty();
        } else {
            ContentKey latestKey = keys.iterator().next();
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
        logger.info("deleting channel " + channelName);
        spokeWriteContentDao.delete(channelName);
        spokeReadContentDao.delete(channelName);
        s3SingleContentDao.delete(channelName);
        s3BatchContentDao.delete(channelName);
        s3LargePayloadContentDao.delete(channelName);
        lastContentPath.delete(channelName, CHANNEL_LATEST_UPDATED);
        lastContentPath.delete(channelName, S3Verifier.LAST_SINGLE_VERIFIED);
        Optional<ChannelConfig> optionalChannelConfig = channelService.getCachedChannelConfig(channelName);
        if (optionalChannelConfig.isPresent() && !optionalChannelConfig.get().isSingle()) {
            new S3Batch(optionalChannelConfig.get(), hubUtils).stop();
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
        if (newConfig.isSingle()) {
            if (oldConfig != null && !oldConfig.isSingle()) {
                new S3Batch(newConfig, hubUtils).stop();
            }
        } else {
            new S3Batch(newConfig, hubUtils).start();
        }
        if (newConfig.isHistorical() && oldConfig != null && oldConfig.isHistorical()) {
            if (newConfig.getMutableTime().isBefore(oldConfig.getMutableTime())) {
                handleMutableTimeChange(newConfig, oldConfig);
            }
        }
    }

    private void handleMutableTimeChange(ChannelConfig newConfig, ChannelConfig oldConfig) {
        ContentPath latest = lastContentPath.get(newConfig.getDisplayName(), ContentKey.NONE, CHANNEL_LATEST_UPDATED);
        logger.info("handleMutableTimeChange {}", latest);
        if (latest.equals(ContentKey.NONE)) {
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
            ActiveTraces.getLocal().log(logger);
            if (mutableLatest.isPresent()) {
                ContentKey mutableKey = mutableLatest.get();
                if (mutableKey.getTime().isAfter(newConfig.getMutableTime())) {
                    logger.info("handleMutableTimeChange.updateIncrease {}", mutableKey);
                    lastContentPath.updateIncrease(mutableKey, newConfig.getDisplayName(), CHANNEL_LATEST_UPDATED);
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
        protected void startUp() throws Exception {
            spokeWriteContentDao.initialize();
            spokeReadContentDao.initialize();
            s3SingleContentDao.initialize();
            s3BatchContentDao.initialize();
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing
        }
    }

    private class ChannelLatestUpdatedService extends AbstractScheduledService {

        @Override
        protected synchronized void runOneIteration() throws Exception {
            logger.debug("running...");
            ActiveTraces.start("ChannelLatestUpdatedService");
            channelService.getChannels().forEach(channelConfig -> {
                try {
                    DateTime time = TimeUtil.stable().plusMinutes(1);
                    Traces traces = new Traces(channelConfig.getDisplayName(), time);
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
                    logger.debug("latest updated {} {}", channelConfig.getDisplayName(), latest);
                    traces.log(logger);
                } catch (Exception e) {
                    logger.warn("unexpected ChannelLatestUpdatedService issue " + channelConfig.getDisplayName(), e);
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
