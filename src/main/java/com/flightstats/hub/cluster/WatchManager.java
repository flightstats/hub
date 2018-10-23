package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
    public WatchManager(CuratorFramework curator, HubProperties hubProperties) {
        this.curator = curator;
        executorService = Executors.newFixedThreadPool(hubProperties.getProperty("watchManager.threads", 10),
                new ThreadFactoryBuilder().setNameFormat("watch-manager-%d").build());
        HubServices.register(new WatchManagerService());
    }

    private class WatchManagerService extends AbstractIdleService {

        @Override
        protected void startUp() {
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
        curator.getCuratorListenable().addListener((client, event) -> {
            logger.info("event {}", event);
            final Watcher watcher = watcherMap.get(event.getPath());
            if (watcher != null) {
                addWatch(watcher);
                if (executorService.isShutdown()) {
                    logger.warn("service is shutdown, skipping event {}", event);
                } else {
                    executorService.submit(() -> {
                        Thread thread = Thread.currentThread();
                        String name = thread.getName();
                        thread.setName("wm-event-" + event.getPath());
                        watcher.callback(event);
                        thread.setName(name);
                    });
                }
            }
        }, Executors.newSingleThreadExecutor());
    }

    public void register(Watcher watcher) {
        String path = watcher.getPath();
        createNode(watcher);
        watcherMap.put(path, watcher);
        addWatch(watcher);
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

    private void addWatch(Watcher watcher) {
        try {
            if (watcher.watchChildren()) {
                curator.getChildren().watched().forPath(watcher.getPath());
            } else {
                curator.getData().watched().forPath(watcher.getPath());
            }
        } catch (Exception e) {
            logger.warn("unable to start watcher", e);
        }
    }

}
