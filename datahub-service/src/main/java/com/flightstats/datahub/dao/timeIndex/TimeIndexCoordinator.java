package com.flightstats.datahub.dao.timeIndex;

import com.flightstats.datahub.cluster.ZooKeeperState;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.jetbrains.annotations.NotNull;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class TimeIndexCoordinator implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(TimeIndexCoordinator.class);

    private final CuratorFramework curator;
    private final TimeIndexDao timeIndexDao;
    private final ZooKeeperState zooKeeperState;

    @Inject
    public TimeIndexCoordinator(CuratorFramework curator, TimeIndexDao timeIndexDao,
                                ZooKeeperState zooKeeperState) {
        this.curator = curator;
        this.timeIndexDao = timeIndexDao;
        this.zooKeeperState = zooKeeperState;
    }

    @Override
    public void run() {
        try {
            List<String> channels = curator.getChildren().forPath(TimeIndex.getPath());
            logger.debug("found " + channels.size() + " channels");
            Collections.shuffle(channels);
            for (String channel : channels) {
                new TimeIndexProcessor(curator, timeIndexDao, zooKeeperState).process(channel);
            }
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node exception " + e.getMessage());
        } catch (Exception e) {
            logger.info("unable to process", e);
        }
    }

    public void startThread() {
        int offset = new Random().nextInt(60);
        Executors.newScheduledThreadPool(1, new ThreadFactory() {
            @NotNull
            @Override
            public Thread newThread(@NotNull Runnable r) {
                return new Thread(r, "TimeIndex" + TimeIndex.getHash(new DateTime()));
            }

        }).scheduleWithFixedDelay(this, offset, 60, TimeUnit.SECONDS);
    }
}
