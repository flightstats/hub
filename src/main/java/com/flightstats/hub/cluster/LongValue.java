package com.flightstats.hub.cluster;

import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LongValue {
    private final static Logger logger = LoggerFactory.getLogger(LongValue.class);

    private final CuratorFramework curator;

    @Inject
    public LongValue(CuratorFramework curator) {
        this.curator = curator;
    }

    public void initialize(String path, long defaultValue) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(path, Longs.toByteArray(defaultValue));
        } catch (KeeperException.NodeExistsException ignore) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            logger.warn("unable to create node", e);
        }
    }

    public long get(String path, long defaultValue) {
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

    private long get(String path) throws Exception {
        return Longs.fromByteArray(curator.getData().forPath(path));
    }

    public void updateIncrease(long next, String path) {
        try {
            int attempts = 0;
            while (attempts < 3) {
                LastUpdated existing = getLastUpdated(path);
                if (next > existing.value) {
                    if (setValue(path, next, existing)) {
                        return;
                    }
                } else {
                    return;
                }
                attempts++;
            }
        } catch (Exception e) {
            logger.warn("unable to set " + path + " lastUpdated to " + next, e);
        }
    }

    private boolean setValue(String path, long next, LastUpdated existing) throws Exception {
        try {
            curator.setData().withVersion(existing.version).forPath(path, Longs.toByteArray(next));
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

    LastUpdated getLastUpdated(String path) {
        try {
            Stat stat = new Stat();
            byte[] bytes = curator.getData().storingStatIn(stat).forPath(path);
            return new LastUpdated(Longs.fromByteArray(bytes), stat.getVersion());
        } catch (KeeperException.NoNodeException e) {
            logger.info("unable to get value " + path + " " + e.getMessage());
            throw new RuntimeException(e);
        } catch (Exception e) {
            logger.info("unable to get value " + path, e);
            throw new RuntimeException(e);
        }
    }

    class LastUpdated {
        long value;
        int version;

        private LastUpdated(long value, int version) {
            this.value = value;
            this.version = version;
        }
    }
}
