package com.flightstats.hub.cluster;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.config.ZooKeeperProperties;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException;

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
public class WatchManager {

    private final ConcurrentHashMap<String, Watcher> watcherMap = new ConcurrentHashMap<>();
    private final CuratorFramework curator;
    private final ExecutorService executorService;

    @Inject
    public WatchManager(CuratorFramework curator, ZooKeeperProperties zookeeperProperties) {
        this.curator = curator;
        this.executorService = Executors.newFixedThreadPool(zookeeperProperties.getWatchManagerThreadCount(),
                new ThreadFactoryBuilder().setNameFormat("watch-manager-%d").build());
        HubServices.register(new WatchManagerService());
    }

    @VisibleForTesting
    void addCuratorListener() {
        curator.getCuratorListenable().addListener((client, event) -> {
            log.info("event {}", event);
            final Watcher watcher = watcherMap.get(event.getPath());
            if (watcher != null) {
                addWatch(watcher);
                if (executorService.isShutdown()) {
                    log.warn("service is shutdown, skipping event {}", event);
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
                log.warn("unable to set watcher path", e);
            }
        }
    }

    private void createNode(Watcher watcher) {
        try {
            curator.create().creatingParentsIfNeeded().forPath(watcher.getPath(), Longs.toByteArray(System.currentTimeMillis()));
        } catch (KeeperException.NodeExistsException ignore) {
            //this will typically happen, except the first time
        } catch (Exception e) {
            log.warn("unable to create node", e);
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
            log.warn("unable to start watcher", e);
        }
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

}
