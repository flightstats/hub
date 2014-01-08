package com.flightstats.datahub.dao.s3;

import com.flightstats.datahub.dao.TimeIndex;
import com.flightstats.datahub.util.TimeProvider;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessSemaphoreMutex;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 */
public class TimeIndexProcessor {

    private final static Logger logger = LoggerFactory.getLogger(TimeIndexProcessor.class);
    private final CuratorFramework curator;
    private String channel;
    private final TimeIndexDao timeIndexDao;
    private final TimeProvider timeProvider;
    private final AtomicBoolean exit = new AtomicBoolean(false);

    @Inject
    public TimeIndexProcessor(CuratorFramework curator, TimeIndexDao timeIndexDao, TimeProvider timeProvider) {
        this.curator = curator;
        this.timeIndexDao = timeIndexDao;
        this.timeProvider = timeProvider;
    }

    public void process(String channel) {
        this.channel = channel;
        InterProcessSemaphoreMutex mutex = new InterProcessSemaphoreMutex(curator, "/TimeIndexLock/" + channel);
        try {
            curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {
                    if (newState.equals(ConnectionState.LOST) || newState.equals(ConnectionState.SUSPENDED)) {
                        exit.set(true);
                        logger.info("setting exit to true" + newState);
                    }
                }
            });

            if (mutex.acquire(1, TimeUnit.MINUTES)) {
                logger.info("acquired " + channel);
                processChannel();
            }
        } catch (Exception e) {
            logger.warn("oh no! " + channel, e);
        } finally {
            try {
                mutex.release();
            } catch (Exception e) {
                //ignore
            }
        }
    }

    private void processChannel() {
        try {
            String path = TimeIndex.getPath(channel);
            List<String> dateHashes = curator.getChildren().forPath(path);
            if (dateHashes.isEmpty()) {
                logger.info("clearing empty path " + path);
                curator.delete().forPath(path);
            } else {
                logger.info("found " + dateHashes.size() + " for " + channel);
                Collections.sort(dateHashes);
                for (String dateHash : dateHashes) {
                    if (exit.get()) {
                        logger.info("exiting " + channel);
                        return;
                    }
                    processDateTime(dateHash);
                }
            }
        } catch (Exception e) {
            logger.warn("unable to process children " + channel, e);
        }
    }

    private void processDateTime(String dateHash) {
        try {
            DateTime dateTime = TimeIndex.parseHash(dateHash);
            DateTime compare = timeProvider.getDateTime().minusMinutes(2);
            if (!dateTime.isBefore(compare)) {
                return;
            }
            String path = TimeIndex.getPath(channel, dateHash);
            List<String> keys = curator.getChildren().forPath(path);
            timeIndexDao.writeIndices(channel, dateHash, keys);
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to process " + channel + " " + dateHash, e);
        }
    }
}
