package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubServices;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WatchManager {
    private final static Logger logger = LoggerFactory.getLogger(WatchManager.class);

    private final CuratorFramework curator;
    private final ExecutorService executorService;
    private final ConcurrentHashMap<String, Watcher> watcherMap = new ConcurrentHashMap<>();

    @Inject
    public WatchManager(CuratorFramework curator) {
        this.curator = curator;
        //todo - gfm - 6/2/14 - do we care what this number is?
        executorService = Executors.newFixedThreadPool(10);
        HubServices.register(new WatchManagerService());
    }

    private class WatchManagerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            addCuratorListener();
        }

        @Override
        protected void shutDown() throws Exception {
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        }

    }

    @VisibleForTesting
    void addCuratorListener() {
        curator.getCuratorListenable().addListener(new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework client, final CuratorEvent event) throws Exception {
                logger.debug("event {}", event);
                final Watcher watcher = watcherMap.get(event.getPath());
                if (watcher != null) {
                    addWatch(watcher.getPath());
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            watcher.callback(event);
                        }
                    });
                }
            }
        });
    }

    public void register(Watcher watcher) {
        String path = watcher.getPath();
        createNode(watcher);
        watcherMap.put(path, watcher);
        addWatch(path);
    }

    public void notifyWatcher(String path) {
        Watcher watcher = watcherMap.get(path);
        if (watcher != null) {
            try {
                curator.setData().forPath(path, Longs.toByteArray(System.currentTimeMillis()));
            } catch (Exception e) {
                logger.warn("unable to set watcher path", e);
            }
        }
    }

    private void createNode(Watcher watcher) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(watcher.getPath(), Longs.toByteArray(System.currentTimeMillis()));
        } catch (KeeperException.NodeExistsException ignore) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            logger.warn("unable to create node", e);
        }
    }

    private void addWatch(String path) {
        try {
            curator.getData().watched().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to start watcher", e);
        }
    }

}
