package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.exception.FailedQueryException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
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
    private final boolean dropSomeWrites = HubProperties.getProperty("s3.dropSomeWrites", false);
    private final int spokeTtlMinutes = HubProperties.getSpokeTtl();
    @Inject
    @Named(ContentDao.CACHE)
    private ContentDao spokeContentDao;
    @Inject
    @Named(ContentDao.SINGLE_LONG_TERM)
    private ContentDao s3SingleContentDao;
    @Inject
    @Named(ContentDao.BATCH_LONG_TERM)
    private ContentDao s3BatchContentDao;
    @Inject
    @Named(ContentDao.LARGE_PAYLOAD)
    private ContentDao largePayloadContentDao;
    @Inject
    private ChannelService channelService;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private S3WriteQueue s3WriteQueue;
    @Inject
    private HubUtils hubUtils;

    private static final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ClusterContentService-%d").build());

    public ClusterContentService() {
        HubServices.registerPreStop(new SpokeS3ContentServiceInit());
        HubServices.register(new ChannelLatestUpdatedService());
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        Content spokeContent = content;
        if (content.isLarge()) {
            largePayloadContentDao.insert(channelName, content);
            spokeContent = createIndex(content);
        }
        ContentKey key = spokeContentDao.insert(channelName, spokeContent);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (channel.isSingle() || channel.isBoth()) {
            Supplier<Void> local = () -> {
                s3SingleWrite(channelName, key);
                return null;
            };
            GlobalChannelService.handleGlobal(channel, local, () -> null, local);
        }
        return key;
    }

    private void s3SingleWrite(String channelName, ContentKey key) {
        if (dropSomeWrites && Math.random() > 0.5) {
            logger.debug("dropping {} {}", channelName, key);
        } else {
            s3WriteQueue.add(new ChannelContentKey(channelName, key));
        }
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        String channelName = bulkContent.getChannel();
        SortedSet<ContentKey> keys = spokeContentDao.insert(bulkContent);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (channel.isSingle() || channel.isBoth()) {
            for (ContentKey key : keys) {
                s3SingleWrite(channelName, key);
            }
        }
        return keys;
    }

    @Override
    public boolean historicalInsert(String channelName, Content content) throws Exception {
        s3SingleContentDao.insertHistorical(channelName, content);
        return true;
    }

    @Override
    public Optional<Content> get(String channelName, ContentKey key) {
        logger.trace("fetching {} from channel {} ", key.toString(), channelName);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (key.getTime().isAfter(getSpokeTtlTime(channelName))) {
            Content content = spokeContentDao.get(channelName, key);
            if (content != null) {
                logger.trace("returning from spoke {} {}", key.toString(), channelName);
                return checkForLargeIndex(channelName, content);
            }
        }
        Content content;
        if (channel.isSingle()) {
            content = s3SingleContentDao.get(channelName, key);
        } else if (channel.isBatch()) {
            content = s3BatchContentDao.get(channelName, key);
        } else {
            content = s3SingleContentDao.get(channelName, key);
            if (content == null) {
                content = s3BatchContentDao.get(channelName, key);
            }
        }
        return checkForLargeIndex(channelName, content);
    }

    private Optional<Content> checkForLargeIndex(String channelName, Content content) {
        if (content == null) {
            return Optional.absent();
        }
        if (content.isIndexForLarge()) {
            ContentKey indexKey = content.getContentKey().get();
            Content largeMeta = fromIndex(content);
            content = largePayloadContentDao.get(channelName, largeMeta.getContentKey().get());
            content.setContentKey(indexKey);
        }
        return Optional.of(content);
    }

    private DateTime getSpokeTtlTime(String channelName) {
        DateTime startTime = channelService.getLastUpdated(channelName, new ContentKey(TimeUtil.now())).getTime();
        return startTime.minusMinutes(spokeTtlMinutes);
    }

    @Override
    public void get(String channelName, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        SortedSet<MinutePath> minutePaths = ContentKeyUtil.convert(keys);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        DateTime spokeTtlTime = getSpokeTtlTime(channelName);
        for (MinutePath minutePath : minutePaths) {
            if (minutePath.getTime().isAfter(spokeTtlTime)
                    || channel.isSingle()) {
                getValues(channelName, callback, minutePath);
            } else {
                if (!s3BatchContentDao.streamMinute(channelName, minutePath, callback)) {
                    getValues(channelName, callback, minutePath);
                }
            }
        }
    }

    private void getValues(String channelName, Consumer<Content> callback, ContentPathKeys contentPathKeys) {
        for (ContentKey contentKey : contentPathKeys.getKeys()) {
            Optional<Content> contentOptional = get(channelName, contentKey);
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
            daos.add(spokeContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM)) {
            daos.add(s3SingleContentDao);
            daos.add(s3BatchContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM_SINGLE)) {
            daos.add(s3SingleContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM_BATCH)) {
            daos.add(s3BatchContentDao);
        } else {
            daos.add(spokeContentDao);
            ChannelConfig channel = channelService.getCachedChannelConfig(query.getChannelName());
            DateTime spokeTtlTime = getSpokeTtlTime(query.getChannelName());
            if (channel.isHistorical() && channel.getMutableTime().isAfter(spokeTtlTime)) {
                spokeTtlTime = channel.getMutableTime();
            }
            if (query.outsideOfCache(spokeTtlTime)) {
                if (channel.isSingle()) {
                    daos.add(s3SingleContentDao);
                } else if (channel.isBatch()) {
                    daos.add(s3BatchContentDao);
                } else {
                    daos.add(s3SingleContentDao);
                    daos.add(s3BatchContentDao);
                }
            }
        }
        return query(daoQuery, daos);
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
            latch.await(118, TimeUnit.SECONDS);
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
        final ChannelConfig cachedChannelConfig = channelService.getCachedChannelConfig(channel);
        DateTime cacheTtlTime = getSpokeTtlTime(channel);
        Optional<ContentKey> latest = spokeContentDao.getLatest(channel, latestQuery.getStartKey(), ActiveTraces.getLocal());
        if (latest.isPresent()) {
            ActiveTraces.getLocal().add("found spoke latest", channel, latest);
            lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            return latest;
        }
        ContentPath latestCache = lastContentPath.get(channel, null, CHANNEL_LATEST_UPDATED);
        ActiveTraces.getLocal().add("found latestCache", channel, latestCache);
        if (latestCache != null) {
            DateTime channelTtlTime = cachedChannelConfig.getTtlTime();
            if(latestCache.getTime().isBefore(channelTtlTime)){
                lastContentPath.update(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            }
            ActiveTraces.getLocal().add("found cached latest", channel, latest);
            if (latestCache.equals(ContentKey.NONE)) {
                return Optional.absent();
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
            return Optional.absent();
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
        spokeContentDao.delete(channelName);
        s3SingleContentDao.delete(channelName);
        s3BatchContentDao.delete(channelName);
        lastContentPath.delete(channelName, CHANNEL_LATEST_UPDATED);
        lastContentPath.delete(channelName, S3Verifier.LAST_SINGLE_VERIFIED);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (!channel.isSingle()) {
            new S3Batch(channel, hubUtils).stop();
        }
    }

    @Override
    public void delete(String channelName, ContentKey contentKey) {
        s3SingleContentDao.delete(channelName, contentKey);
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        s3SingleContentDao.deleteBefore(name, limitKey);
        s3BatchContentDao.deleteBefore(name, limitKey);
    }

    @Override
    public void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        if (oldConfig == null) {
            lastContentPath.updateIncrease(ContentKey.NONE, newConfig.getName(), CHANNEL_LATEST_UPDATED);
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
        ContentPath latest = lastContentPath.get(newConfig.getName(), ContentKey.NONE, CHANNEL_LATEST_UPDATED);
        logger.info("handleMutableTimeChange {}", latest);
        if (latest.equals(ContentKey.NONE)) {
            DirectionQuery query = DirectionQuery.builder()
                    .startKey(ContentKey.lastKey(oldConfig.getMutableTime().plusMillis(1)))
                    .earliestTime(newConfig.getMutableTime())
                    .channelName(newConfig.getName())
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
                    lastContentPath.updateIncrease(mutableKey, newConfig.getName(), CHANNEL_LATEST_UPDATED);
                }
            }
        }
    }

    private class SpokeS3ContentServiceInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            spokeContentDao.initialize();
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
                    Traces traces = new Traces(channelConfig.getName(), time);
                    DirectionQuery latestQuery = DirectionQuery.builder()
                            .channelName(channelConfig.getName())
                            .next(false)
                            .stable(false)
                            .location(Location.ALL)
                            .epoch(Epoch.IMMUTABLE)
                            .startKey(ContentKey.lastKey(time))
                            .count(1)
                            .build();
                    Optional<ContentKey> latest = getLatest(latestQuery);
                    logger.debug("latest updated {} {}", channelConfig.getName(), latest);
                    traces.log(logger);
                } catch (Exception e) {
                    logger.warn("unexpected ChannelLatestUpdatedService issue " + channelConfig.getName(), e);
                }
            });
            ActiveTraces.end();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 59, TimeUnit.MINUTES);
        }

    }

}
