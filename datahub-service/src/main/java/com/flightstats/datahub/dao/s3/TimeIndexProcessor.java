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

/**
 *
 */
public class TimeIndexProcessor {

    private final static Logger logger = LoggerFactory.getLogger(TimeIndexProcessor.class);
    private final CuratorFramework curator;
    private final String channel;
    private final TimeIndexDao timeIndexDao;
    private final TimeProvider timeProvider;

    @Inject
    public TimeIndexProcessor(CuratorFramework curator, String channel, TimeIndexDao timeIndexDao, TimeProvider timeProvider) {
        this.curator = curator;
        this.channel = channel;
        this.timeIndexDao = timeIndexDao;
        this.timeProvider = timeProvider;
    }

    public void process() {
        InterProcessSemaphoreMutex mutex = new InterProcessSemaphoreMutex(curator, "/TimeIndexLock/" + channel);
        try {
            curator.getConnectionStateListenable().addListener(new ConnectionStateListener() {
                @Override
                public void stateChanged(CuratorFramework client, ConnectionState newState) {
                    logger.info("state changed " + newState);
                    /**
                     * todo - gfm - 1/6/14 - handle
                     * It is strongly recommended that you add a ConnectionStateListener and watch for SUSPENDED and LOST state changes.
                     * If a SUSPENDED state is reported you cannot be certain that you still hold the lock unless you subsequently receive a RECONNECTED state.
                     * If a LOST state is reported it is certain that you no longer hold the lock.
                     */
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
            List<String> dateHashes = curator.getChildren().forPath(TimeIndex.getPath(channel));
            Collections.sort(dateHashes);
            for (String dateHash : dateHashes) {
                processDateTime(dateHash);
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
