package com.flightstats.hub.cluster;

import com.flightstats.hub.model.ContentPath;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.commons.io.Charsets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LastContentPath {
    private final static Logger logger = LoggerFactory.getLogger(LastContentPath.class);

    private final CuratorFramework curator;

    @Inject
    public LastContentPath(CuratorFramework curator) {
        this.curator = curator;
    }

    public void initialize(String name, ContentPath defaultPath, String basePath) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(basePath + name, defaultPath.toBytes());
        } catch (KeeperException.NodeExistsException ignore) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            logger.warn("unable to create node", e);
        }
    }

    public ContentPath getOrNull(String name, String basePath) {
        String path = basePath + name;
        try {
            return get(path);
        } catch (Exception e) {
            logger.info("unable to get node " + e.getMessage());
            return null;
        }
    }

    public ContentPath get(String name, ContentPath defaultPath, String basePath) {
        String path = basePath + name;
        try {
            return get(path);
        } catch (KeeperException.NoNodeException e) {
            logger.warn("missing value for {}", name);
            initialize(name, defaultPath, basePath);
            return get(name, defaultPath, basePath);
        } catch (Exception e) {
            logger.warn("unable to get node " + e.getMessage());
            return defaultPath;
        }
    }

    private ContentPath get(String path) throws Exception {
        byte[] bytes = curator.getData().forPath(path);
        return ContentPath.fromUrl(new String(bytes, Charsets.UTF_8)).get();
    }

    public void updateIncrease(ContentPath nextPath, String name, String basePath) {
        String path = basePath + name;
        try {
            while (true) {
                LastUpdated existing = getLastUpdated(path);
                if (nextPath.compareTo(existing.key) > 0) {
                    if (setValue(path, nextPath, existing)) {
                        return;
                    }
                } else {
                    return;
                }
            }
        } catch (KeeperException.NoNodeException e) {
            logger.info("values does not exist, creating {}", path);
            initialize(name, nextPath, basePath);
        } catch (Exception e) {
            logger.warn("unable to set " + path + " lastUpdated to " + nextPath, e);
        }
    }

    private boolean setValue(String path, ContentPath nextPath, LastUpdated existing) throws Exception {
        try {
            curator.setData().withVersion(existing.version).forPath(path, nextPath.toBytes());
            return true;
        } catch (KeeperException.BadVersionException e) {
            logger.debug("bad version " + path + " " + e.getMessage());
            return false;
        } catch (Exception e) {
            logger.info("what happened? " + path, e);
            return false;
        }
    }

    public void delete(String name, String basePath) {
        String path = basePath + name;
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete {} {}", path, e.getMessage());
        }
    }

    LastUpdated getLastUpdated(String path) throws Exception {
        Stat stat = new Stat();
        byte[] bytes = curator.getData().storingStatIn(stat).forPath(path);
        Optional<ContentPath> pathOptional = ContentPath.fromUrl(new String(bytes, Charsets.UTF_8));
        return new LastUpdated(pathOptional.get(), stat.getVersion());
    }

    class LastUpdated {
        ContentPath key;
        int version;

        private LastUpdated(ContentPath key, int version) {
            this.key = key;
            this.version = version;
        }
    }
}
