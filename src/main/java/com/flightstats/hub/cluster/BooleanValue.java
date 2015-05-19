package com.flightstats.hub.cluster;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
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

    public boolean setIfNotValue(String path, boolean value) {
        try {
            int attempts = 0;
            while (attempts < 3) {
                LastUpdated existing = getLastUpdated(path);
                if (value != existing.value) {
                    if (setValue(path, value, existing)) {
                        return true;
                    }
                } else {
                    return false;
                }
                attempts++;
            }
        } catch (Exception e) {
            logger.warn("unable to set " + path + " lastUpdated to " + value, e);
        }
        return false;
    }

    LastUpdated getLastUpdated(String path) {
        try {
            Stat stat = new Stat();
            byte[] bytes = curator.getData().storingStatIn(stat).forPath(path);
            return new LastUpdated(getBoolean(bytes), stat.getVersion());
        } catch (KeeperException.NoNodeException e) {
            logger.info("unable to get value " + path + " " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.info("unable to get value " + path, e);
            throw new RuntimeException(e);
        }
    }

    class LastUpdated {
        boolean value;
        int version;

        private LastUpdated(boolean value, int version) {
            this.value = value;
            this.version = version;
        }
    }

    private boolean setValue(String path, boolean value, LastUpdated existing) throws Exception {
        try {
            curator.setData().withVersion(existing.version).forPath(path, getBytes(value));
            return true;
        } catch (KeeperException.BadVersionException e) {
            logger.info("bad version " + path + " " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.info("what happened? " + path, e);
            return false;
        }
    }

    private boolean get(String path) throws Exception {
        byte[] bytes = curator.getData().forPath(path);
        return getBoolean(bytes);
    }

    private boolean getBoolean(byte[] bytes) {
        if (bytes.length == 1) {
            return bytes[0] == 1;
        }
        return false;
    }

    public void delete(String path) {
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete {} {}", path, e.getMessage());
        }
    }

}
