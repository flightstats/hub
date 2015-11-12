package com.flightstats.hub.group;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class GroupProcessorImpl implements GroupProcessor {
    private final static Logger logger = LoggerFactory.getLogger(GroupProcessorImpl.class);

    private static final String WATCHER_PATH = "/groupCallback/watcher";

    private final WatchManager watchManager;
    private final GroupService groupService;
    private final Provider<GroupLeader> leaderProvider;
    private LastContentPath lastContentPath;
    private final Map<String, GroupLeader> activeGroups = new HashMap<>();

    @Inject
    public GroupProcessorImpl(WatchManager watchManager, GroupService groupService,
                              Provider<GroupLeader> leaderProvider, LastContentPath lastContentPath) {
        this.watchManager = watchManager;
        this.groupService = groupService;
        this.leaderProvider = leaderProvider;
        this.lastContentPath = lastContentPath;
        HubServices.registerPreStop(new GroupCallbackService());
    }

    private void start() {
        logger.info("starting");
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                manageGroups();
            }

            @Override
            public String getPath() {
                return WATCHER_PATH;
            }

        });
        manageGroups();
    }

    private synchronized void manageGroups() {
        Set<String> groupsToStop = new HashSet<>(activeGroups.keySet());
        Iterable<Group> groups = groupService.getGroups();
        for (Group group : groups) {
            groupsToStop.remove(group.getName());
            GroupLeader activeLeader = activeGroups.get(group.getName());
            if (activeLeader == null) {
                startGroup(group);
            } else if (activeLeader.getGroup().isChanged(group)) {
                logger.info("changed group {}", group);
                activeGroups.remove(group.getName());
                activeLeader.exit(false);
                startGroup(group);
            } else {
                logger.debug("group not changed {}", group);
            }
        }
        stop(groupsToStop, true);
    }

    private void stop(Set<String> groupsToStop, final boolean delete) {
        List<Callable<Object>> groups = new ArrayList<>();
        logger.info("stopping groups {}", groupsToStop);
        for (String groupToStop : groupsToStop) {
            logger.info("stopping " + groupToStop);
            final GroupLeader groupLeader = activeGroups.remove(groupToStop);
            groups.add(() -> {
                groupLeader.exit(delete);
                return null;
            });
        }
        try {
            List<Future<Object>> futures = Executors.newCachedThreadPool().invokeAll(groups, 90, TimeUnit.SECONDS);
            logger.info("stopped groups " + futures);
        } catch (InterruptedException e) {
            logger.warn("interrupted! ", e);
            throw new RuntimeInterruptedException(e);
        }
    }

    private void startGroup(Group group) {
        logger.trace("starting group {}", group);
        GroupLeader groupLeader = leaderProvider.get();
        groupLeader.tryLeadership(group);
        activeGroups.put(group.getName(), groupLeader);
    }

    @Override
    public void delete(String name) {
        GroupLeader groupLeader = activeGroups.get(name);
        notifyWatchers();
        if (groupLeader != null) {
            logger.info("deleting...{}", groupLeader);
            for (int i = 0; i < 30; i++) {
                if (groupLeader.deleteIfReady()) {
                    logger.info("deleted successfully! " + name);
                    return;
                } else {
                    Sleeper.sleep(1000);
                    logger.info("waiting to delete " + name);
                }
            }
            groupLeader.deleteAnyway();
        }

    }

    @Override
    public void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    @Override
    public void getStatus(Group group, GroupStatus.GroupStatusBuilder statusBuilder) {
        statusBuilder.lastCompleted(lastContentPath.get(group.getName(), GroupStrategy.createContentPath(group), GroupLeader.GROUP_LAST_COMPLETED));
        GroupLeader groupLeader = activeGroups.get(group.getName());
        if (groupLeader != null) {
            statusBuilder.errors(groupLeader.getErrors());
            statusBuilder.inFlight(groupLeader.getInFlight(group));
        } else {
            statusBuilder.errors(Collections.emptyList());
            statusBuilder.inFlight(Collections.emptyList());
        }
    }

    private class GroupCallbackService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            start();
        }

        @Override
        protected void shutDown() throws Exception {
            stop(new HashSet<>(activeGroups.keySet()), false);
        }

    }
}
