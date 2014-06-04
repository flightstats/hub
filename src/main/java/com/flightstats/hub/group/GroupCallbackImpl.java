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
import java.util.Map;

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
            //todo - gfm - 6/3/14 - implement this
        }

    }

    private void start() {
        logger.info("starting");
        watchManager.register(new Watcher() {
            @Override
            public void callback(CuratorEvent event) {
                startGroups();
            }

            @Override
            public String getPath() {
                return WATCHER_PATH;
            }

        });
        startGroups();
    }

    private synchronized void startGroups() {
        Iterable<Group> groups = groupService.getGroups();
        for (Group group : groups) {
            if (!activeGroups.containsKey(group.getName())) {
                startGroup(group);
            }
        }
        //todo - gfm - 6/3/14 - this should stop active groups after deletion.

    }

    private void startGroup(Group group) {
        GroupCaller groupCaller = callerProvider.get();
        groupCaller.tryLeadership(group);
        activeGroups.put(group.getName(), groupCaller);

    }

    //todo - gfm - 6/3/14 - should this get called when channels are created also?
    @Override
    public void notifyWatchers() {
        watchManager.notifyWatcher(WATCHER_PATH);
    }

}
