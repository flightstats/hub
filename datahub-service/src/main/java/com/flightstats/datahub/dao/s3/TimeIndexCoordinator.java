package com.flightstats.datahub.dao.s3;

import com.flightstats.datahub.dao.TimeIndex;
import com.flightstats.datahub.util.TimeProvider;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class TimeIndexCoordinator implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(TimeIndexCoordinator.class);

    private final CuratorFramework curator;
    private final TimeIndexDao timeIndexDao;
    private final TimeProvider timeProvider;

    @Inject
    public TimeIndexCoordinator(CuratorFramework curator, TimeIndexDao timeIndexDao, TimeProvider timeProvider) {
        this.curator = curator;
        this.timeIndexDao = timeIndexDao;
        this.timeProvider = timeProvider;
    }

    @Override
    public void run() {
        /**
         * todo - gfm - 1/7/14 -
         * INFO 2014-01-07 18:55:23,972 [pool-3-thread-1] com.flightstats.datahub.dao.s3.TimeIndexCoordinator [line 40] - unable to process
         org.apache.zookeeper.KeeperException$NoNodeException: KeeperErrorCode = NoNode for /deihub/TimeIndex
         at org.apache.zookeeper.KeeperException.create(KeeperException.java:111) ~[zookeeper-3.4.5.jar:3.4.5-1392090]
         at org.apache.zookeeper.KeeperException.create(KeeperException.java:51) ~[zookeeper-3.4.5.jar:3.4.5-1392090]
         at org.apache.zookeeper.ZooKeeper.getChildren(ZooKeeper.java:1586) ~[zookeeper-3.4.5.jar:3.4.5-1392090]
         at org.apache.curator.framework.imps.GetChildrenBuilderImpl$3.call(GetChildrenBuilderImpl.java:214) ~[curator-framework-2.3.0.jar:na]
         at org.apache.curator.framework.imps.GetChildrenBuilderImpl$3.call(GetChildrenBuilderImpl.java:203) ~[curator-framework-2.3.0.jar:na]
         at org.apache.curator.RetryLoop.callWithRetry(RetryLoop.java:107) ~[curator-client-2.3.0.jar:na]
         at org.apache.curator.framework.imps.GetChildrenBuilderImpl.pathInForeground(GetChildrenBuilderImpl.java:199) ~[curator-framework-2.3.0.jar:na]
         at org.apache.curator.framework.imps.GetChildrenBuilderImpl.forPath(GetChildrenBuilderImpl.java:191) ~[curator-framework-2.3.0.jar:na]
         at org.apache.curator.framework.imps.GetChildrenBuilderImpl.forPath(GetChildrenBuilderImpl.java:38) ~[curator-framework-2.3.0.jar:na]
         at com.flightstats.datahub.dao.s3.TimeIndexCoordinator.run(TimeIndexCoordinator.java:33) ~[datahub-service-0.1.7.jar:na]
         */
        try {
            List<String> channels = curator.getChildren().forPath(TimeIndex.getPath());
            logger.debug("found " + channels.size() + " channels");
            Collections.shuffle(channels);
            for (String channel : channels) {
                new TimeIndexProcessor(curator, timeIndexDao, timeProvider).process(channel);
            }
        } catch (Exception e) {
            logger.info("unable to process", e);
        }
    }
}
