package com.flightstats.hub.group;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.flightstats.hub.model.ContentKey;
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

public class GroupCallbackImpl implements GroupCallback {
    private final static Logger logger = LoggerFactory.getLogger(GroupCallbackImpl.class);

    private static final String WATCHER_PATH = "/groupCallback/watcher";

    private final WatchManager watchManager;
    private final GroupService groupService;
    private final Provider<GroupCaller> callerProvider;
    private final Map<String, GroupCaller> activeGroups = new HashMap<>();

    @Inject
    public GroupCallbackImpl(WatchManager watchManager, GroupService groupService, Provider<GroupCaller> callerProvider) {
        this.watchManager = watchManager;
        this.groupService = groupService;
        this.callerProvider = callerProvider;
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
            if (!activeGroups.containsKey(group.getName())) {
                startGroup(group);
            }
        }
        stop(groupsToStop, true);
    }

    private void stop(Set<String> groupsToStop, final boolean delete) {
        List<Callable<Object>> groups = new ArrayList<>();
        logger.info("stopping groups {}", groupsToStop);
        for (String groupToStop : groupsToStop) {
            logger.info("stopping " + groupToStop);
            final GroupCaller groupCaller = activeGroups.remove(groupToStop);
            groups.add(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    groupCaller.exit(delete);
                    return null;
                }
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
        GroupCaller groupCaller = callerProvider.get();
        groupCaller.tryLeadership(group);
        activeGroups.put(group.getName(), groupCaller);

    }

    @Override
    public void delete(String name) {
        GroupCaller groupCaller = activeGroups.get(name);
        notifyWatchers();
        if (groupCaller != null) {
            logger.info("deleting...{}", groupCaller);
            for (int i = 0; i < 30; i++) {
                if (groupCaller.deleteIfReady()) {
                    logger.info("deleted successfully! " + name);
                    return;
                } else {
                    Sleeper.sleep(1000);
                    logger.info("waiting to delete " + name);
                }
            }
            groupCaller.deleteAnyway();
        }

    }

    @Override
    public void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    @Override
    public void getStatus(Group group, GroupStatus.GroupStatusBuilder statusBuilder) {
        GroupCaller groupCaller = activeGroups.get(group.getName());
        if (groupCaller != null) {
            statusBuilder.lastCompleted(groupCaller.getLastCompleted());
            statusBuilder.errors(groupCaller.getErrors());
            statusBuilder.inFlight(groupCaller.getInFlight());
        } else {
            statusBuilder.lastCompleted(ContentKey.NONE);
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
