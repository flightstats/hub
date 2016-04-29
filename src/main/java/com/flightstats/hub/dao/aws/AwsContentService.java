package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.exception.FailedWriteException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ChannelReplicator;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

public class AwsContentService implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(AwsContentService.class);

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
    private ChannelService channelService;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private S3WriteQueue s3WriteQueue;
    @Inject
    private HubUtils hubUtils;

    private static final String CHANNEL_LATEST_UPDATED = "/ChannelLatestUpdated/";

    private final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("AwsContentService-%d").build());
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Integer shutdown_wait_seconds = HubProperties.getProperty("app.shutdown_wait_seconds", 5);
    private final boolean dropSomeWrites = HubProperties.getProperty("s3.dropSomeWrites", false);
    private final int spokeTtlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);

    public AwsContentService() {
        HubServices.registerPreStop(new AwsContentServiceInit());
        HubServices.register(new ChannelLatestUpdatedService());
    }

    void waitForInFlight() {
        logger.info("waiting for in-flight to complete " + inFlight.get());
        long start = System.currentTimeMillis();
        while (inFlight.get() > 0) {
            logger.info("still waiting for in-flight to complete " + inFlight.get());
            Sleeper.sleep(1000);
            if (System.currentTimeMillis() > (start + shutdown_wait_seconds * 1000)) {
                break;
            }
        }
        logger.info("completed waiting for in-flight to complete " + inFlight.get());
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        try {
            inFlight.incrementAndGet();
            ContentKey key = spokeContentDao.write(channelName, content);
            ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
            if (channel.isSingle() || channel.isBoth()) {
                s3SingleWrite(channelName, key);
            }
            return key;
        } finally {
            inFlight.decrementAndGet();
        }
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
        MultiPartParser multiPartParser = new MultiPartParser(bulkContent);
        multiPartParser.parse();
        try {
            return newBulkWrite(bulkContent);
        } catch (FailedWriteException e) {
            logger.info("failed bulk write, fall back");
            Collection<ContentKey> keys = new ArrayList<>();
            for (Content content : bulkContent.getItems()) {
                keys.add(insert(bulkContent.getChannel(), content));
            }
            return keys;
        }
    }

    private Collection<ContentKey> newBulkWrite(BulkContent bulkContent) throws Exception {
        String channelName = bulkContent.getChannel();
        try {
            inFlight.incrementAndGet();
            SortedSet<ContentKey> keys = spokeContentDao.write(bulkContent);
            ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
            if (channel.isSingle() || channel.isBoth()) {
                for (ContentKey key : keys) {
                    s3SingleWrite(channelName, key);
                }
            }
            return keys;
        } finally {
            inFlight.decrementAndGet();
        }
    }

    @Override
    public Optional<Content> getValue(String channelName, ContentKey key) {
        logger.trace("fetching {} from channel {} ", key.toString(), channelName);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (key.getTime().isAfter(getCacheTtlTime(channelName, channel))) {
            Content content = spokeContentDao.read(channelName, key);
            if (content != null) {
                logger.trace("returning from spoke {} {}", key.toString(), channelName);
                return Optional.of(content);
            }
        }
        Content content = null;
        if (channel.isSingle()) {
            content = s3SingleContentDao.read(channelName, key);
        } else if (channel.isBatch()) {
            content = s3BatchContentDao.read(channelName, key);
        } else {
            content = s3SingleContentDao.read(channelName, key);
            if (content == null) {
                content = s3BatchContentDao.read(channelName, key);
            }
        }
        return Optional.fromNullable(content);
    }

    private DateTime getCacheTtlTime(String channelName, ChannelConfig channel) {
        DateTime startTime = TimeUtil.now();
        if (channel.isReplicating()) {
            startTime = lastContentPath.get(channelName, MinutePath.NONE, ChannelReplicator.REPLICATED_LAST_UPDATED).getTime();
        }
        return startTime.minusMinutes(spokeTtlMinutes);
    }

    @Override
    public void getValues(String channelName, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        //todo - gfm - 4/14/16 - this may come in as seconds...
        SortedSet<MinutePath> minutePaths = ContentKeyUtil.convert(keys);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        DateTime cacheTtlTime = getCacheTtlTime(channelName, channel);
        for (MinutePath minutePath : minutePaths) {
            if (minutePath.getTime().isAfter(cacheTtlTime)
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
            Optional<Content> contentOptional = getValue(channelName, contentKey);
            if (contentOptional.isPresent()) {
                callback.accept(contentOptional.get());
            }
        }
    }

    private Runnable readRunner(ContentDao contentDao, String channelName, ContentKey key,
                                Queue<Content> queue, CountDownLatch latch) {
        Traces traces = ActiveTraces.getLocal();
        return () -> {
            ActiveTraces.setLocal(traces);
            Content content = contentDao.read(channelName, key);
            if (content != null) {
                queue.add(content);
                latch.countDown();
            }
            latch.countDown();
        };
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
        if (query.getLocation().equals(Location.CACHE)) {
            return query(daoQuery, spokeContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM)) {
            return query(daoQuery, s3SingleContentDao, s3BatchContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM_SINGLE)) {
            return query(daoQuery, s3SingleContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM_BATCH)) {
            return query(daoQuery, s3BatchContentDao);
        } else {
            //todo - gfm - 11/12/15 - this could exclude Spoke for time queries over a specific age
            ChannelConfig channel = channelService.getCachedChannelConfig(query.getChannelName());
            if (channel.isSingle()) {
                return query(daoQuery, spokeContentDao, s3SingleContentDao);
            } else if (channel.isBatch()) {
                return query(daoQuery, spokeContentDao, s3BatchContentDao);
            } else {
                return query(daoQuery, spokeContentDao, s3SingleContentDao, s3BatchContentDao);
            }
        }
    }

    private SortedSet<ContentKey> query(Function<ContentDao, SortedSet<ContentKey>> daoQuery, ContentDao... contentDaos) {
        SortedSet<ContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        try {
            CountDownLatch latch = new CountDownLatch(contentDaos.length);
            Traces traces = ActiveTraces.getLocal();
            String threadName = Thread.currentThread().getName();
            for (ContentDao contentDao : contentDaos) {
                executorService.submit((Runnable) () -> {
                    Thread.currentThread().setName(contentDao.getClass().getSimpleName() + "|" + threadName);
                    ActiveTraces.setLocal(traces);
                    orderedKeys.addAll(daoQuery.apply(contentDao));
                    latch.countDown();
                });
            }
            latch.await(118, TimeUnit.SECONDS);
            return orderedKeys;
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces, boolean stable) {
        final ChannelConfig cachedChannelConfig = channelService.getCachedChannelConfig(channel);
        DateTime cacheTtlTime = getCacheTtlTime(channel, cachedChannelConfig);
        Optional<ContentKey> latest = spokeContentDao.getLatest(channel, limitKey, traces);
        if (latest.isPresent()) {
            logger.info("found latest {} {}", channel, latest);
            lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            return latest;
        }
        ContentPath latestCache = lastContentPath.get(channel, null, CHANNEL_LATEST_UPDATED);
        if (latestCache != null) {
            DateTime channelTtlTime = TimeUtil.getEarliestTime((int) cachedChannelConfig.getTtlDays());
            if(latestCache.getTime().isBefore(channelTtlTime)){
                lastContentPath.update(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            }
            logger.info("found cached {} {}", channel, latestCache);
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
                .count(1)
                .build();
        Collection<ContentKey> keys = channelService.getKeys(query);
        if (keys.isEmpty()) {
            logger.debug("updating channel empty {}", channel);
            lastContentPath.updateIncrease(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            return Optional.absent();
        } else {
            ContentKey latestKey = keys.iterator().next();
            if (latestKey.getTime().isAfter(cacheTtlTime)) {
                logger.debug("latestKey within spoke window {} {}", channel, latestKey);
                lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            } else {
                logger.debug("updating cache with latestKey {} {}", channel, latestKey);
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
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (!channel.isSingle()) {
            new S3Batch(channel, hubUtils).stop();
        }
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        s3SingleContentDao.deleteBefore(name, limitKey);
        s3BatchContentDao.deleteBefore(name, limitKey);
    }

    @Override
    public void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        if (newConfig.isSingle()) {
            if (oldConfig != null && !oldConfig.isSingle()) {
                new S3Batch(newConfig, hubUtils).stop();
            }
        } else {
            new S3Batch(newConfig, hubUtils).start();
        }
    }

    private class AwsContentServiceInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            spokeContentDao.initialize();
            s3SingleContentDao.initialize();
            s3BatchContentDao.initialize();
        }

        @Override
        protected void shutDown() throws Exception {
            waitForInFlight();
        }
    }

    private class ChannelLatestUpdatedService extends AbstractScheduledService {

        @Override
        protected synchronized void runOneIteration() throws Exception {
            logger.debug("running...");
            List<String> names = lastContentPath.getNames(StringUtils.removeEnd(CHANNEL_LATEST_UPDATED, "/"));
            for (String name : names) {
                try {
                    DateTime time = TimeUtil.stable().plusMinutes(1);
                    Traces traces = new Traces(name, time);
                    Optional<ContentKey> latest = getLatest(name, ContentKey.lastKey(time), traces, false);
                    logger.debug("latest updated {} {}", name, latest);
                    traces.log(logger);
                } catch (Exception e) {
                    logger.warn("unexpected ChannelLatestUpdatedService issue " + name, e);
                }
            }

        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 59, TimeUnit.MINUTES);
        }

    }

}
