package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("Convert2Lambda")
public class ContentServiceImpl implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

    private final ContentDao cacheContentDao;
    private final ContentDao longTermContentDao;
    private final int ttlMinutes;
    private final Integer shutdown_wait_seconds;
    private final AtomicInteger inFlight = new AtomicInteger();
    private final ExecutorService executorService;

    @Inject
    public ContentServiceImpl(@Named(ContentDao.CACHE) ContentDao cacheContentDao,
                              @Named(ContentDao.LONG_TERM) ContentDao longTermContentDao,
                              @Named("spoke.ttlMinutes") int ttlMinutes,
                              @Named("app.shutdown_wait_seconds") Integer shutdown_wait_seconds) {
        this.cacheContentDao = cacheContentDao;
        this.longTermContentDao = longTermContentDao;
        this.ttlMinutes = ttlMinutes;
        this.shutdown_wait_seconds = shutdown_wait_seconds;
        executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ContentServiceImpl-%d").build());
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
    public ContentKey insert(String channelName, Content content) {
        try {
            inFlight.incrementAndGet();
            return cacheContentDao.write(channelName, content);
        } finally {
            inFlight.decrementAndGet();
        }
    }

    @Override
    public Optional<Content> getValue(String channelName, ContentKey key) {
        logger.trace("fetching {} from channel {} ", key.toString(), channelName);
        //todo - gfm - 12/22/14 - this should also handle the case of replication,
        // where something is in the cache outside the 'cache window'
        return getBoth(channelName, key);
    }

    private Optional<Content> getBoth(String channelName, ContentKey key) {
        ConcurrentLinkedQueue<Content> queue = new ConcurrentLinkedQueue<>();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    Content content = cacheContentDao.read(channelName, key);
                    if (content != null) {
                        queue.add(content);
                        countDownLatch.countDown();
                    }
                    countDownLatch.countDown();
                }
            });
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    Content content = longTermContentDao.read(channelName, key);
                    if (content != null) {
                        queue.add(content);
                        countDownLatch.countDown();
                    }
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
            return Optional.fromNullable(queue.poll());
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery query) {
        if (query.getLocation().equals(Location.CACHE)) {
            return cacheContentDao.queryByTime(query.getChannelName(), query.getStartTime(), query.getUnit(), query.getTraces());
        } else if (query.getLocation().equals(Location.LONG_TERM)) {
            return longTermContentDao.queryByTime(query.getChannelName(), query.getStartTime(), query.getUnit(), query.getTraces());
        } else {
            return queryBothByTime(query);
        }
    }

    private SortedSet<ContentKey> queryBothByTime(TimeQuery query) {
        SortedSet<ContentKey> orderedKeys = new TreeSet<>();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(cacheContentDao.queryByTime(query.getChannelName(), query.getStartTime(), query.getUnit(), query.getTraces()));
                    countDownLatch.countDown();
                }
            });
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(longTermContentDao.queryByTime(query.getChannelName(), query.getStartTime(), query.getUnit(), query.getTraces()));
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
        cacheContentDao.delete(channelName);
        longTermContentDao.delete(channelName);
    }

    @Override
    public Collection<ContentKey> getKeys(DirectionQuery query) {
        if (query.getLocation().equals(Location.CACHE)) {
            return getKeys(query, cacheContentDao, "cache");
        } else if (query.getLocation().equals(Location.LONG_TERM)) {
            return getKeys(query, longTermContentDao, "s3");
        } else {
            return queryBoth(query);
        }
    }

    private Set<ContentKey> queryBoth(DirectionQuery query) {
        SortedSet<ContentKey> orderedKeys = new TreeSet<>();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(getKeys(query, cacheContentDao, "cache"));
                    countDownLatch.countDown();
                }
            });
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(getKeys(query, longTermContentDao, "s3"));
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await(3, TimeUnit.MINUTES);
            query.getTraces().add("both unique keys", orderedKeys);
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
            cacheContentDao.initialize();
            longTermContentDao.initialize();
        }

        @Override
        protected void shutDown() throws Exception {
            waitForInFlight();
        }
    }


}
