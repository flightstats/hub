package com.flightstats.hub.group;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.WatchManager;
import com.flightstats.hub.cluster.Watcher;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.curator.framework.api.CuratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
        HubServices.register(new GroupCallbackService());
    }

    private class GroupCallbackService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            start();
        }

        @Override
        protected void shutDown() throws Exception {
            stop(new HashSet<>(activeGroups.keySet()));
        }

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
        //todo - gfm - 6/9/14 - look for changed groups?
        stop(groupsToStop);
    }

    private void stop(Set<String> groupsToStop) {
        for (String groupToStop : groupsToStop) {
            logger.info("stopping " + groupToStop);
            GroupCaller groupCaller = activeGroups.remove(groupToStop);
            groupCaller.exit();
        }
    }

    private void startGroup(Group group) {
        GroupCaller groupCaller = callerProvider.get();
        groupCaller.tryLeadership(group);
        activeGroups.put(group.getName(), groupCaller);

    }

    @Override
    public void delete(String name) {
        GroupCaller groupCaller = activeGroups.get(name);
        if (groupCaller != null) {
            groupCaller.delete();
        }
        notifyWatchers();
    }

    @Override
    public void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

    public long getLastCompleted(Group group) {
        GroupCaller groupCaller = activeGroups.get(group.getName());
        if (groupCaller != null) {
            return groupCaller.getLastCompleted();
        }
        return 0;
    }
}
