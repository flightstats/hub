package com.flightstats.hub.dao.timeIndex;

import com.flightstats.hub.cluster.CuratorLock;
import com.flightstats.hub.cluster.Lockable;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class TimeIndexProcessor implements Lockable {

    private final static Logger logger = LoggerFactory.getLogger(TimeIndexProcessor.class);
    private final CuratorLock curatorLock;
    private String channel;
    private final TimeIndexDao timeIndexDao;
    private final CuratorFramework curator;

    @Inject
    public TimeIndexProcessor(CuratorLock curatorLock, TimeIndexDao timeIndexDao, CuratorFramework curator) {
        this.curatorLock = curatorLock;
        this.timeIndexDao = timeIndexDao;
        this.curator = curator;
    }

    public void process(String channel) {
        this.channel = channel;
        curatorLock.runWithLock(this, "/TimeIndexLock/" + channel, 1, TimeUnit.SECONDS);
    }

    @Override
    public void runWithLock() {
        try {
            String path = TimeIndex.getPath(channel);
            List<String> dateHashes = curator.getChildren().forPath(path);
            if (dateHashes.isEmpty()) {
                logger.debug("clearing empty path " + path);
                //this will fail if a new record has already been written.
                curator.delete().forPath(path);
            } else if (dateHashes.size() > 2) {
                logger.debug("found {} for {}", dateHashes.size(), channel);
                Collections.sort(dateHashes);
                dateHashes = dateHashes.subList(0, dateHashes.size() - 2);
                for (String dateHash : dateHashes) {
                    if (curatorLock.shouldStopWorking()) {
                        logger.info("exiting {}" + channel);
                        return;
                    }
                    processDateTime(dateHash);
                }
            }
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node exception " + channel + " " + e.getMessage());
        } catch (Exception e) {
            logger.warn("unable to process children " + channel, e);
        }
    }

    private void processDateTime(String dateHash) {
        try {
            String path = TimeIndex.getPath(channel, dateHash);
            List<String> keys = curator.getChildren().forPath(path);
            timeIndexDao.writeIndices(channel, dateHash, keys);
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to process " + channel + " " + dateHash, e);
        }
    }

}
