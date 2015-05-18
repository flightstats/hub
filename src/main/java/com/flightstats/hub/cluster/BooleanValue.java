package com.flightstats.hub.cluster;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BooleanValue {
    private final static Logger logger = LoggerFactory.getLogger(BooleanValue.class);

    private final CuratorFramework curator;

    @Inject
    public BooleanValue(CuratorFramework curator) {
        this.curator = curator;
    }

    public void initialize(String path, boolean defaultValue) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(path, getBytes(defaultValue));
        } catch (KeeperException.NodeExistsException ignore) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            logger.warn("unable to create node", e);
        }
    }

    private byte[] getBytes(boolean value) {
        return new byte[]{(byte) (value ? 1 : 0)};
    }

    public boolean get(String path, boolean defaultValue) {
        try {
            return get(path);
        } catch (KeeperException.NoNodeException e) {
            logger.warn("missing value for {}", path);
            initialize(path, defaultValue);
            return get(path, defaultValue);
        } catch (Exception e) {
            logger.warn("unable to get node " + e.getMessage());
            return defaultValue;
        }
    }

    private boolean get(String path) throws Exception {
        byte[] bytes = curator.getData().forPath(path);
        if (bytes.length == 1) {
            return bytes[0] == 1;
        }
        return false;
    }

    public boolean setValue(String path, boolean value) throws Exception {
        try {
            curator.setData().forPath(path, getBytes(value));
            return true;
        } catch (KeeperException.BadVersionException e) {
            logger.info("bad version " + path + " " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.info("what happened? " + path, e);
            return false;
        }
    }

    public void delete(String path) {
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete {} {}", path, e.getMessage());
        }
    }

}
