package com.flightstats.hub.group;

import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.data.Stat;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

@Singleton
class GroupError {
    private final static Logger logger = LoggerFactory.getLogger(GroupError.class);
    private static final int MAX_SIZE = 10;

    private final CuratorFramework curator;

    @Inject
    public GroupError(CuratorFramework curator) {
        this.curator = curator;
    }

    public void add(String group, String error) {
        String path = getErrorRoot(group) + "/" + TimeUtil.now().getMillis() + RandomStringUtils.randomAlphanumeric(6);
        try {
            curator.create().creatingParentsIfNeeded().forPath(path, error.getBytes());
        } catch (Exception e) {
            logger.warn("unable to create " + path, e);
        }
        limitChildren(group);
    }

    private void limitChildren(String group) {
        String errorRoot = getErrorRoot(group);
        try {
            List<String> children = curator.getChildren().forPath(errorRoot);
            children.sort(String.CASE_INSENSITIVE_ORDER);
            if (children.size() > MAX_SIZE) {
                int transition = children.size() - MAX_SIZE;
                for (int i = 0; i < transition; i++) {
                    curator.delete().forPath(getChildPath(errorRoot, children.get(i)));
                }
                DateTime cutoffTime = TimeUtil.now().minusHours(1);
                for (int i = transition; i < children.size(); i++) {
                    Stat stat = new Stat();
                    String child = children.get(i);
                    curator.getData().storingStatIn(stat).forPath(getChildPath(errorRoot, child));
                    if (new DateTime(stat.getCtime()).isBefore(cutoffTime)) {
                        curator.delete().forPath(getChildPath(errorRoot, child));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("unable to limit children " + errorRoot, e);
        }
    }

    public void delete(String group) {
        String errorRoot = getErrorRoot(group);
        logger.info("deleting " + errorRoot);
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(errorRoot);
        } catch (KeeperException.NoNodeException e) {
            logger.info("unable to delete missing node " + errorRoot);
        } catch (Exception e) {
            logger.warn("unable to delete " + errorRoot, e);
        }
    }

    private String getErrorRoot(String group) {
        return "/GroupError/" + group;
    }

    private String getChildPath(String errorRoot, String child) {
        return errorRoot + "/" + child;
    }

    public List<String> get(String group) {
        String errorRoot = getErrorRoot(group);
        List<String> errors = new ArrayList<>();
        try {
            Collection<String> children = new TreeSet<>(curator.getChildren().forPath(errorRoot));
            for (String child : children) {
                errors.add(new String(curator.getData().forPath(getChildPath(errorRoot, child))));
            }
        } catch (KeeperException.NoNodeException e) {
            logger.info("unable to get missing node " + errorRoot);
        } catch (Exception e) {
            logger.warn("unable to get children " + errorRoot, e);
        }
        return errors;
    }
}
