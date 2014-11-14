package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.Sleeper;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class ContentServiceImpl implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

    private final ContentDao contentDao;
    private final ContentDao longTermContentDao;
    private final int spokeTtlMinutes;
    private final Integer shutdown_wait_seconds;
    private final AtomicInteger inFlight = new AtomicInteger();

    @Inject
    public ContentServiceImpl(@Named(ContentDao.CACHE) ContentDao cacheContentDao,
                              @Named(ContentDao.LONG_TERM) ContentDao longTermContentDao,
                              @Named("spoke.ttl_minutes") int spokeTtlMinutes,
                              @Named("app.shutdown_wait_seconds") Integer shutdown_wait_seconds) {
        this.contentDao = cacheContentDao;
        this.longTermContentDao = longTermContentDao;
        this.spokeTtlMinutes = spokeTtlMinutes;
        this.shutdown_wait_seconds = shutdown_wait_seconds;
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
    public void createChannel(ChannelConfiguration configuration) {
        logger.info("Creating channel " + configuration);
        contentDao.initializeChannel(configuration);
    }

    @Override
    public ContentKey insert(ChannelConfiguration configuration, Content content) {
        try {
            inFlight.incrementAndGet();
            String channelName = configuration.getName();
            logger.trace("inserting {} bytes into channel {} ", content.getData().length, channelName);
            //todo - gfm - 11/14/14 - always write to cache
            return contentDao.write(channelName, content);
        } finally {
            inFlight.decrementAndGet();
        }
    }

    @Override
    public Optional<Content> getValue(String channelName, ContentKey key) {
        logger.trace("fetching {} from channel {} ", key.toString(), channelName);
        //todo - gfm - 11/14/14 - figure out where to look based on cacheTtlHours
        Content value = contentDao.read(channelName, key);
        return Optional.fromNullable(value);
    }

    @Override
    public Optional<ContentKey> findLastUpdatedKey(String channelName) {
        //todo - gfm - 11/14/14 - look in cache first, then S3
        //todo - gfm - 10/28/14 - implement
        return Optional.absent();
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime startTime, DateTime endTime) {
        //todo - gfm - 11/14/14 - figure out where to look based on cacheTtlHours
        return contentDao.getKeys(channelName, startTime, endTime);
    }

    @Override
    public void delete(String channelName) {
        logger.info("deleting channel " + channelName);
        //todo - gfm - 11/14/14 - delete in both
        contentDao.delete(channelName);
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count) {
        //todo - gfm - 11/14/14 - figure out where to look based on cacheTtlHours, may need to span
        return contentDao.getKeys(channelName, contentKey, count);
    }

    private class ContentServiceHook extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            //todo - gfm - 11/14/14 - call init on both Daos
        }

        @Override
        protected void shutDown() throws Exception {
            waitForInFlight();
        }
    }


}
