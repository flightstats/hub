package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.ChannelReplicator;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class AwsContentService implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(AwsContentService.class);

    @Inject
    @Named(ContentDao.CACHE)
    private ContentDao spokeContentDao;
    @Inject
    @Named(ContentDao.LONG_TERM)
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

    private final ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("AwsContentService-%d").build());
    private final AtomicInteger inFlight = new AtomicInteger();
    private final Integer shutdown_wait_seconds = HubProperties.getProperty("app.shutdown_wait_seconds", 5);
    private final boolean dropSomeWrites = HubProperties.getProperty("s3.dropSomeWrites", false);
    private final int ttlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);

    public AwsContentService() {
        HubServices.registerPreStop(new ContentServiceHook());
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
            ChannelConfig channel = channelService.getChannelConfig(channelName);
            if (channel.isSingle() || channel.isBoth()) {
                s3SingleWrite(channelName, key);
            }
            return key;
        } finally {
            inFlight.decrementAndGet();
        }
    }

    private void s3SingleWrite(String channelName, ContentKey key) {
        if (dropSomeWrites) {
            if (Math.random() < 0.95) {
                s3WriteQueue.add(new ChannelContentKey(channelName, key));
            }
        } else {
            s3WriteQueue.add(new ChannelContentKey(channelName, key));
        }
    }

    @Override
    public Collection<ContentKey> insert(String channelName, BatchContent batchContent) throws Exception {
        Collection<ContentKey> keys = new ArrayList<>();
        MultiPartParser multiPartParser = new MultiPartParser(batchContent);
        multiPartParser.parse();
        for (Content content : batchContent.getItems()) {
            keys.add(insert(channelName, content));
        }
        return keys;
    }

    @Override
    public Optional<Content> getValue(String channelName, ContentKey key) {
        logger.trace("fetching {} from channel {} ", key.toString(), channelName);
        ChannelConfig channel = channelService.getChannelConfig(channelName);
        DateTime startTime = TimeUtil.now();
        if (channel.isReplicating()) {
            startTime = lastContentPath.get(channelName, MinutePath.NONE, ChannelReplicator.REPLICATED_LAST_UPDATED).getTime();
        }
        if (startTime.minusMinutes(ttlMinutes).isAfter(key.getTime())) {
            Content content = spokeContentDao.read(channelName, key);
            if (content != null) {
                return Optional.of(content);
            }
        }
        Content content = null;
        if (channel.isSingle()) {
            content = s3SingleContentDao.read(channelName, key);
            if (content == null) {
                content = s3BatchContentDao.read(channelName, key);
            }
        } else if (channel.isBatch()) {
            content = s3BatchContentDao.read(channelName, key);
            if (content != null) {
                content = s3SingleContentDao.read(channelName, key);
            }
        } else {
            ConcurrentLinkedQueue<Content> queue = new ConcurrentLinkedQueue<>();
            try {
                CountDownLatch latch = new CountDownLatch(2);
                executorService.submit(readRunner(s3BatchContentDao, channelName, key, queue, latch));
                executorService.submit(readRunner(s3SingleContentDao, channelName, key, queue, latch));
                latch.await();
                return Optional.fromNullable(queue.poll());
            } catch (InterruptedException e) {
                throw new RuntimeInterruptedException(e);
            }
        }
        return Optional.fromNullable(content);
    }

    private Runnable readRunner(ContentDao contentDao, String channelName, ContentKey key,
                                Queue<Content> queue, CountDownLatch latch) {
        return () -> {
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
        if (query.getLocation().equals(Location.CACHE)) {
            return spokeContentDao.queryByTime(query.getChannelName(), query.getStartTime(), query.getUnit(), query.getTraces());
        } else if (query.getLocation().equals(Location.LONG_TERM)) {
            return s3SingleContentDao.queryByTime(query.getChannelName(), query.getStartTime(), query.getUnit(), query.getTraces());
        } else {
            return queryBothByTime(query);
        }
    }

    private SortedSet<ContentKey> queryBothByTime(TimeQuery query) {
        SortedSet<ContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(spokeContentDao.queryByTime(query.getChannelName(), query.getStartTime(), query.getUnit(), query.getTraces()));
                    countDownLatch.countDown();
                }
            });
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(s3SingleContentDao.queryByTime(query.getChannelName(), query.getStartTime(), query.getUnit(), query.getTraces()));
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await(3, TimeUnit.MINUTES);
            return orderedKeys;
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    @Override
    public void delete(String channelName) {
        logger.info("deleting channel " + channelName);
        spokeContentDao.delete(channelName);
        s3SingleContentDao.delete(channelName);
    }

    @Override
    public Collection<ContentKey> getKeys(DirectionQuery query) {
        if (query.getLocation().equals(Location.CACHE)) {
            return getKeys(query, spokeContentDao, "cache");
        } else if (query.getLocation().equals(Location.LONG_TERM)) {
            return getKeys(query, s3SingleContentDao, "s3");
        } else {
            return queryBoth(query);
        }
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        return spokeContentDao.getLatest(channel, limitKey, traces);
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        s3SingleContentDao.deleteBefore(name, limitKey);
    }

    private Set<ContentKey> queryBoth(DirectionQuery query) {
        SortedSet<ContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(getKeys(query, spokeContentDao, "cache"));
                    countDownLatch.countDown();
                }
            });
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(getKeys(query, s3SingleContentDao, "s3"));
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await(3, TimeUnit.MINUTES);
            return orderedKeys;
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    private SortedSet<ContentKey> getKeys(DirectionQuery query, ContentDao dao, String name) {
        SortedSet<ContentKey> keys = dao.query(query);
        query.getTraces().add(name, keys);
        return keys;
    }

    private class ContentServiceHook extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            spokeContentDao.initialize();
            s3SingleContentDao.initialize();
        }

        @Override
        protected void shutDown() throws Exception {
            waitForInFlight();
        }
    }


}
