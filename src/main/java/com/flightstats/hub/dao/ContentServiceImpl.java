package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.Sleeper;
import com.flightstats.hub.websocket.WebsocketPublisher;
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
    private final ContentDao cacheDao;
    private final LastUpdatedDao lastUpdatedDao;
    private final WebsocketPublisher websocketPublisher;
    private final Integer shutdown_wait_seconds;
    private final AtomicInteger inFlight = new AtomicInteger();

    @Inject
    public ContentServiceImpl(@Named(ContentDao.LONG_TERM_STORE) ContentDao contentDao,
                              @Named(ContentDao.SHORT_TERM_CACHE) ContentDao cacheDao,
                              LastUpdatedDao lastUpdatedDao,
                              WebsocketPublisher websocketPublisher,
                              @Named("app.shutdown_wait_seconds") Integer shutdown_wait_seconds) {
        this.contentDao = contentDao;
        this.cacheDao = cacheDao;
        this.lastUpdatedDao = lastUpdatedDao;
        this.websocketPublisher = websocketPublisher;
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
    public InsertedContentKey insert(ChannelConfiguration configuration, Content content) {
        try {
            inFlight.incrementAndGet();
            String channelName = configuration.getName();
            //todo - gfm - 10/28/14 - make this a more interesting info level log
            logger.debug("inserting {} bytes into channel {} ", content.getData().length, channelName);

            InsertedContentKey result = contentDao.write(channelName, content, configuration.getTtlDays());
            //todo - gfm - 10/28/14 - remove this
            lastUpdatedDao.update(channelName, result.getKey());
            //todo - gfm - 10/28/14 - change this
            websocketPublisher.publish(channelName, result.getKey());
            return result;
        } finally {
            inFlight.decrementAndGet();
        }
    }

    @Override
    public Optional<LinkedContent> getValue(String channelName, String id) {
        Optional<ContentKey> keyOptional = contentDao.getKey(id);
        if (!keyOptional.isPresent()) {
            return Optional.absent();
        }
        ContentKey key = keyOptional.get();
        logger.debug("fetching {} from channel {} ", key.toString(), channelName);
        Content value = contentDao.read(channelName, key);
        if (value == null) {
            return Optional.absent();
        }
        ContentKey next = key.getNext();
        if (next != null) {
            Optional<ContentKey> lastUpdatedKey = findLastUpdatedKey(channelName);
            if (lastUpdatedKey.isPresent()) {
                if (lastUpdatedKey.get().equals(key)) {
                    next = null;
                }
            }
        }

        return Optional.of(new LinkedContent(value, key.getPrevious(), next));
    }

    @Override
    public Optional<ContentKey> findLastUpdatedKey(String channelName) {
        return Optional.fromNullable(lastUpdatedDao.getLastUpdated(channelName));
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        return contentDao.getKeys(channelName, dateTime);
    }

    @Override
    public void delete(String channelName) {
        logger.info("deleting channel " + channelName);
        contentDao.delete(channelName);
        lastUpdatedDao.delete(channelName);
    }

    private class ContentServiceHook extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
        }

        @Override
        protected void shutDown() throws Exception {
            waitForInFlight();
        }
    }


}
