package com.flightstats.hub.cluster;

import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class LongSet {
    private final String path;
    private final CuratorFramework curator;

    @Inject
    public LongSet(String path, CuratorFramework curator) {
        this.path = path;
        this.curator = curator;
        createNode();
    }

    public static void delete(String path, CuratorFramework curator) {
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            log.warn("unable to delete {} {}", path, e.getMessage());
        }
    }

    private void createNode() {
        try {
            curator.create().creatingParentsIfNeeded().forPath(path);
        } catch (KeeperException.NodeExistsException ignore) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            log.warn("unable to create node", e);
        }
    }

    public void add(long value) {
        try {
            curator.create().forPath(getValuePath(value));
        } catch (KeeperException.NodeExistsException ignore) {
            log.trace("node exists " + getValuePath(value));
        } catch (Exception e) {
            log.warn("unable to create " + getValuePath(value), e);
        }
    }

    public void remove(long value) {
        try {
            curator.delete().forPath(getValuePath(value));
        } catch (Exception e) {
            log.warn("unable to delete " + getValuePath(value), e);
        }
    }

    public Set<Long> getSet() {
        Set<Long> longs = new HashSet<>();
        try {
            List<String> strings = curator.getChildren().forPath(path);
            for (String string : strings) {
                longs.add(Long.valueOf(string));
            }
        } catch (Exception e) {
            log.warn("unable to get set " + path, e);
        }
        return longs;
    }

    private String getValuePath(long value) {
        return path + "/" + value;
    }
}
