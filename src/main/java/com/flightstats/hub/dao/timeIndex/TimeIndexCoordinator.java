package com.flightstats.hub.dao.timeIndex;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorLock;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class TimeIndexCoordinator {
    private final static Logger logger = LoggerFactory.getLogger(TimeIndexCoordinator.class);

    private final CuratorFramework curator;
    private final TimeIndexDao timeIndexDao;
    private final CuratorLock curatorLock;

    @Inject
    public TimeIndexCoordinator(CuratorFramework curator, TimeIndexDao timeIndexDao,
                                CuratorLock curatorLock) {
        this.curator = curator;
        this.timeIndexDao = timeIndexDao;
        this.curatorLock = curatorLock;
        HubServices.register(new TimeIndexCoordinatorService());
    }

    private class TimeIndexCoordinatorService extends AbstractScheduledService {

        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(new Random().nextInt(60), 60, TimeUnit.SECONDS);
        }
    }

    public void run() {
        try {
            List<String> channels = curator.getChildren().forPath(TimeIndex.getPath());
            logger.debug("found " + channels.size() + " channels");
            Collections.shuffle(channels);
            for (String channel : channels) {
                new TimeIndexProcessor(curatorLock, timeIndexDao, curator).process(channel);
            }
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node exception " + e.getMessage());
        } catch (Exception e) {
            logger.info("unable to process", e);
        }
    }

}
