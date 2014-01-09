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

    /**
     * todo - gfm - 1/7/14 -
     * WARN 2014-01-07 19:27:14,701 [pool-3-thread-1] com.flightstats.datahub.dao.s3.TimeIndexProcessor [line 86] - unable to process children testy6
     org.apache.zookeeper.KeeperException$NoNodeException: KeeperErrorCode = NoNode for /deihub/TimeIndex/testy6
     at org.apache.zookeeper.KeeperException.create(KeeperException.java:111) ~[zookeeper-3.4.5.jar:3.4.5-1392090]
     at org.apache.zookeeper.KeeperException.create(KeeperException.java:51) ~[zookeeper-3.4.5.jar:3.4.5-1392090]
     at org.apache.zookeeper.ZooKeeper.getChildren(ZooKeeper.java:1586) ~[zookeeper-3.4.5.jar:3.4.5-1392090]
     at org.apache.curator.framework.imps.GetChildrenBuilderImpl$3.call(GetChildrenBuilderImpl.java:214) ~[curator-framework-2.3.0.jar:na]
     at org.apache.curator.framework.imps.GetChildrenBuilderImpl$3.call(GetChildrenBuilderImpl.java:203) ~[curator-framework-2.3.0.jar:na]
     at org.apache.curator.RetryLoop.callWithRetry(RetryLoop.java:107) ~[curator-client-2.3.0.jar:na]
     at org.apache.curator.framework.imps.GetChildrenBuilderImpl.pathInForeground(GetChildrenBuilderImpl.java:199) ~[curator-framework-2.3.0.jar:na]
     at org.apache.curator.framework.imps.GetChildrenBuilderImpl.forPath(GetChildrenBuilderImpl.java:191) ~[curator-framework-2.3.0.jar:na]
     at org.apache.curator.framework.imps.GetChildrenBuilderImpl.forPath(GetChildrenBuilderImpl.java:38) ~[curator-framework-2.3.0.jar:na]
     at com.flightstats.datahub.dao.s3.TimeIndexProcessor.processChannel(TimeIndexProcessor.java:70) [datahub-service-0.1.7.jar:na]
     at com.flightstats.datahub.dao.s3.TimeIndexProcessor.process(TimeIndexProcessor.java:54) [datahub-service-0.1.7.jar:na]
     */
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
                logger.debug("acquired " + channel);
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
                logger.debug("clearing empty path " + path);
                curator.delete().forPath(path);
            } else {
                logger.debug("found " + dateHashes.size() + " for " + channel);
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
