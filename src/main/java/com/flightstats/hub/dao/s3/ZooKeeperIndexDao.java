package com.flightstats.hub.dao.s3;

import com.flightstats.hub.dao.timeIndex.TimeIndex;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;

public class ZooKeeperIndexDao {

    private final static Logger logger = LoggerFactory.getLogger(ZooKeeperIndexDao.class);

    private final CuratorFramework curator;

    @Inject
    public ZooKeeperIndexDao(CuratorFramework curator) {
        this.curator = curator;
    }

    public void writeIndex(String channelName, DateTime dateTime, ContentKey key) {
        final String path = TimeIndex.getPath(channelName, dateTime, key);
        try {
            curator.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
        } catch (KeeperException.NodeExistsException ignore) {
            //this can happen with rolling restarts
            logger.info("node exists " + path);
        } catch (Exception e) {
            logger.warn("unable to create " + path, e);
            throw new RuntimeException(e);
        }
    }

    Collection<ContentKey> getKeys(String channelName, String hashTime) throws Exception {
        List<String> ids = curator.getChildren().forPath(TimeIndex.getPath(channelName, hashTime));
        return IndexUtils.convertIds(ids);
    }

    void delete(String channelName) {
        String path = TimeIndex.getPath(channelName);
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete path " + path, e);
        }
    }
}
