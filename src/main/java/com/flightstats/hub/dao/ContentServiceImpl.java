package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQuery;
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

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
            logger.trace("inserting {} bytes into channel {} ", content.getData().length, channelName);
            return cacheContentDao.write(channelName, content);
        } finally {
            inFlight.decrementAndGet();
        }
    }

    @Override
    public Optional<Content> getValue(String channelName, ContentKey key) {
        logger.trace("fetching {} from channel {} ", key.toString(), channelName);
        if (isInsideCacheWindow(key.getTime())) {
            return Optional.fromNullable(cacheContentDao.read(channelName, key));
        }
        return Optional.fromNullable(longTermContentDao.read(channelName, key));
    }

    private boolean isInsideCacheWindow(DateTime dateTime) {
        return TimeUtil.now().minusMinutes(ttlMinutes).isBefore(dateTime);
    }

    @Override
    public Optional<ContentKey> findLastUpdatedKey(String channelName) {
        //todo - gfm - 11/14/14 - look in cache first, then S3
        //todo - gfm - 10/28/14 - implement
        return Optional.absent();
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery timeQuery) {
        Set<ContentKey> orderedKeys = new TreeSet<>();
        if (timeQuery.getLocation().equals(Location.CACHE)) {
            orderedKeys.addAll(cacheContentDao.queryByTime(timeQuery.getChannelName(), timeQuery.getStartTime(), timeQuery.getUnit()));
        } else if (timeQuery.getLocation().equals(Location.LONG_TERM)) {
            orderedKeys.addAll(longTermContentDao.queryByTime(timeQuery.getChannelName(), timeQuery.getStartTime(), timeQuery.getUnit()));
        } else if (isInsideCacheWindow(timeQuery.getStartTime())) {
            //todo - gfm - 11/21/14 - is this distinction really needed?
            orderedKeys.addAll(cacheContentDao.queryByTime(timeQuery.getChannelName(), timeQuery.getStartTime(), timeQuery.getUnit()));
        } else {
            queryBothByTime(timeQuery, orderedKeys);
        }
        return orderedKeys;
    }

    private void queryBothByTime(TimeQuery timeQuery, Set<ContentKey> orderedKeys) {
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(cacheContentDao.queryByTime(timeQuery.getChannelName(), timeQuery.getStartTime(), timeQuery.getUnit()));
                    countDownLatch.countDown();
                }
            });
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    orderedKeys.addAll(longTermContentDao.queryByTime(timeQuery.getChannelName(), timeQuery.getStartTime(), timeQuery.getUnit()));
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await();
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
    public Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count) {
        //todo - gfm - 11/14/14 - figure out where to look based on cacheTtlHours, may need to span
        return cacheContentDao.getKeys(channelName, contentKey, count);
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
