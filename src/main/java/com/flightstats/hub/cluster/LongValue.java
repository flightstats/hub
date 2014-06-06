package com.flightstats.hub.cluster;

import com.google.common.primitives.Longs;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongValue {
    private final static Logger logger = LoggerFactory.getLogger(LongValue.class);

    private final String path;
    private final long defaultValue;
    private final CuratorFramework curator;

    public LongValue(String path, long defaultValue, CuratorFramework curator) {
        this.path = path;
        this.defaultValue = defaultValue;
        this.curator = curator;
        createNode();
    }

    private void createNode() {
        try {
            curator.create().creatingParentsIfNeeded().forPath(path, Longs.toByteArray(defaultValue));
        } catch (KeeperException.NodeExistsException ignore ) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            logger.warn("unable to create node", e);
        }
    }

    public long get() {
        try {
            return Longs.fromByteArray(curator.getData().forPath(path));
        } catch (Exception e) {
            logger.warn("unable to get node" + e.getMessage());
            return defaultValue;
        }
    }

    public void update(long next)  {
        try {
            curator.setData().forPath(path, Longs.toByteArray(next));
        } catch (Exception e) {
            logger.warn("unable to set latest {} {}", path, next);
        }
    }


    public void delete()  {
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete {}", path);
        }
    }
}
