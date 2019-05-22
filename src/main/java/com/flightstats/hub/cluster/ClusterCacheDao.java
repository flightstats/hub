package com.flightstats.hub.cluster;

import com.flightstats.hub.config.AppProperties;
import com.flightstats.hub.exception.ConflictException;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.model.ContentPath;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

@Slf4j
public class ClusterCacheDao {

    private final CuratorFramework curator;
    private final AppProperties appProperties;

    @Inject
    public ClusterCacheDao(CuratorFramework curator, AppProperties appProperties) {
        this.curator = curator;
        this.appProperties = appProperties;
    }

    private void trace(String nameOrPath, String text, Object... context) {
        if (log.isTraceEnabled()) {
            if (nameOrPath.contains(appProperties.getLastContentPathTracing())) {
                log.trace(text + " nameorPath " + nameOrPath, context);
            }
        }
    }

    public void initialize(String name, ContentPath defaultPath, String basePath) {
        try {
            trace(name, "initialize {} {}", defaultPath, basePath);
            curator.create().creatingParentsIfNeeded().forPath(basePath + name, defaultPath.toBytes());
        } catch (KeeperException.NodeExistsException ignore) {
            //this will typically happen, except the first time
            log.trace("initialize exists {} {} {}", name, defaultPath, basePath);
        } catch (Exception e) {
            log.warn("unable to create node " + name + " " + basePath, e);
        }
    }

    public ContentPath getOrNull(String name, String basePath) {
        String path = basePath + name;
        try {
            return getMostRecentData(path).getKey();
        } catch (Exception e) {
            log.info("unable to get node {} {} {} ", name, basePath, e.getMessage());
            log.trace("unable to get node  " + path, e);
            return null;
        }
    }

    public ContentPath get(String name, ContentPath defaultPath, String basePath) {
        String path = basePath + name;
        try {
            ContentPath contentPath = getMostRecentData(path).getKey();
            trace(name, "get default {} found {}", defaultPath, contentPath);
            return contentPath;
        } catch (KeeperException.NoNodeException e) {
            if (defaultPath == null) {
                trace(name, "get default {} null", defaultPath);
                return null;
            } else {
                log.warn("missing value for {} {}", name, basePath);
                initialize(name, defaultPath, basePath);
                return get(name, defaultPath, basePath);
            }
        } catch (Exception e) {
            log.info("unable to get node {} {} {} ", name, basePath, e.getMessage());
            return defaultPath;
        }
    }

    public void setIfBefore(ContentPath nextPath, String name, String basePath) {
        setPathValueIf(nextPath, name, basePath, (existing) -> nextPath.compareTo(existing.key) < 0);
    }

    public void setIfAfter(ContentPath nextPath, String name, String basePath) {
        setPathValueIf(nextPath, name, basePath, (existing) -> nextPath.compareTo(existing.key) > 0);
    }

    private void setPathValueIf(ContentPath nextPath, String name, String basePath, Function<MostRecentData, Boolean> compare) {
        String path = basePath + name;
        try {
            while (true) {
                trace(name, "update {}", name);
                MostRecentData existing = getMostRecentData(path);
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
            log.info("values does not exist, creating {}", path);
            trace(name, "updateIncrease NoNodeException {}", name);
            initialize(name, nextPath, basePath);
        } catch (ConflictException e) {
            trace(name, "ConflictException " + e.getMessage());
            throw e;
        } catch (ContentTooLargeException e) {
            throw e;
        } catch (Exception e) {
            log.warn("unable to set lastUpdated " + path, e);
        }
    }

    public void set(ContentPath nextPath, String name, String basePath) {
        String path = basePath + name;
        try {
            MostRecentData existing = getMostRecentData(path);
            setValue(path, nextPath, existing);
            trace(path, "update {} next {} existing{}", path, nextPath, existing);
        } catch (KeeperException.NoNodeException e) {
            log.info("values does not exist, creating {}", path);
            initialize(name, nextPath, basePath);
        } catch (Exception e) {
            log.warn("unable to set " + path + " lastUpdated to " + nextPath, e);
        }
    }

    private boolean setValue(String path, ContentPath nextPath, MostRecentData existing) {
        try {
            curator.setData()
                    .withVersion(existing.getVersion())
                    .forPath(path, nextPath.toBytes());
            return true;
        } catch (KeeperException.BadVersionException e) {
            log.debug("bad version " + path + " " + e.getMessage());
            return false;
        } catch (Exception e) {
            log.info("what happened? " + path, e);
            return false;
        }
    }

    public void delete(String name, String basePath) {
        log.info("delete {} {}", name, basePath);
        String path = basePath + name;
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (KeeperException.NoNodeException e) {
            log.info("no node for {}", path);
        } catch (Exception e) {
            log.warn("unable to delete {} {}", path, e.getMessage());
        }
    }

    private MostRecentData getMostRecentData(String path) throws Exception {
        Stat stat = new Stat();
        byte[] bytes = curator.getData().storingStatIn(stat).forPath(path);
        Optional<ContentPath> pathOptional = ContentPath.fromUrl(new String(bytes, StandardCharsets.UTF_8));
        return new MostRecentData(pathOptional.get(), stat.getVersion());
    }

    private class MostRecentData {
        private ContentPath key;
        private int version;

        private MostRecentData(ContentPath key, int version) {
            this.key = key;
            this.version = version;
        }

        public ContentPath getKey() {
            return key;
        }

        public int getVersion() {
            return version;
        }
    }
}