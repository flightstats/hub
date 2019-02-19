package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.model.ContentPath;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

public class LastContentPath {
    private final static Logger logger = LoggerFactory.getLogger(LastContentPath.class);

    private static final String tracing = HubProperties.getProperty("LastContentPathTracing", "channelToTrace");

    private final CuratorFramework curator;

    @Inject
    public LastContentPath(CuratorFramework curator) {
        this.curator = curator;
    }

    private void trace(String nameOrPath, String text, Object... context) {
        if (logger.isTraceEnabled()) {
            if (nameOrPath.contains(tracing)) {
                logger.trace(text + " nameorPath " + nameOrPath, context);
            }
        }
    }

    public void initialize(String name, ContentPath defaultPath, String basePath) {
        try {
            trace(name, "initialize {} {}", defaultPath, basePath);
            curator.create().creatingParentsIfNeeded().forPath(basePath + name, defaultPath.toBytes());
        } catch (KeeperException.NodeExistsException ignore) {
            //this will typically happen, except the first time
            logger.trace("initialize exists {} {} {}", name, defaultPath, basePath);
        } catch (Exception e) {
            logger.warn("unable to create node " + name + " " + basePath, e);
        }
    }

    public ContentPath getOrNull(String name, String basePath) {
        String path = basePath + name;
        try {
            return get(path);
        } catch (Exception e) {
            logger.info("unable to get node {} {} {} ", name, basePath, e.getMessage());
            logger.trace("unable to get node  " + path, e);
            return null;
        }
    }

    public ContentPath get(String name, ContentPath defaultPath, String basePath) {
        String path = basePath + name;
        try {
            ContentPath contentPath = get(path);
            trace(name, "get default {} found {}", defaultPath, contentPath);
            return contentPath;
        } catch (KeeperException.NoNodeException e) {
            if (defaultPath == null) {
                trace(name, "get default {} null", defaultPath);
                return null;
            } else {
                logger.warn("missing value for {} {}", name, basePath);
                initialize(name, defaultPath, basePath);
                return get(name, defaultPath, basePath);
            }
        } catch (Exception e) {
            logger.info("unable to get node {} {} {} ", name, basePath, e.getMessage());
            return defaultPath;
        }
    }

    private ContentPath get(String path) throws Exception {
        byte[] bytes = curator.getData().forPath(path);
        String found = new String(bytes, StandardCharsets.UTF_8);
        trace(path, "get found {}", found);
        return ContentPath.fromUrl(found).get();
    }

    public void updateDecrease(ContentPath nextPath, String name, String basePath) {
        update(nextPath, name, basePath, (existing) -> nextPath.compareTo(existing.key) < 0);
    }

    public void updateIncrease(ContentPath nextPath, String name, String basePath) {
        update(nextPath, name, basePath, (existing) -> nextPath.compareTo(existing.key) > 0);
    }

    private void update(ContentPath nextPath, String name, String basePath, Function<LastUpdated, Boolean> compare) {
        String path = basePath + name;
        try {
            while (true) {
                trace(name, "update {}", name);
                LastUpdated existing = getLastUpdated(path);
                if (compare.apply(existing)) {
                    if (setValue(path, nextPath, existing)) {
                        trace(name, "update set {} next {} existing {}", name, nextPath, existing);
                        return;
                    }
                } else {
                    trace(name, "update false {} next {} existing{}", name, nextPath, existing);
                    return;
                }
            }
        } catch (KeeperException.NoNodeException e) {
            logger.info("values does not exist, creating {}", path);
            trace(name, "updateIncrease NoNodeException {}", name);
            initialize(name, nextPath, basePath);
        } catch (ConflictException e) {
            trace(name, "ConflictException " + e.getMessage());
            throw e;
        } catch (ContentTooLargeException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("unable to set lastUpdated " + path, e);
        }
    }

    public void update(ContentPath nextPath, String name, String basePath) {
        String path = basePath + name;
        try {
            LastUpdated existing = getLastUpdated(path);
            setValue(path, nextPath, existing);
            trace(path, "update {} next {} existing{}", path, nextPath, existing);
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
        logger.info("delete {} {}", name, basePath);
        String path = basePath + name;
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            logger.info("no node for {}", path);
        } catch (Exception e) {
            logger.warn("unable to delete {} {}", path, e.getMessage());
        }
    }

    private LastUpdated getLastUpdated(String path) throws Exception {
        Stat stat = new Stat();
        byte[] bytes = curator.getData().storingStatIn(stat).forPath(path);
        Optional<ContentPath> pathOptional = ContentPath.fromUrl(new String(bytes, StandardCharsets.UTF_8));
        return new LastUpdated(pathOptional.get(), stat.getVersion());
    }

    private class LastUpdated {
        ContentPath key;
        int version;

        private LastUpdated(ContentPath key, int version) {
            this.key = key;
            this.version = version;
        }
    }
}
