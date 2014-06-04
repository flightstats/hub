package com.flightstats.hub.cluster;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SingleWatcher asynchronously calls the Watcher in a single thread.
 */
//todo - gfm - 6/3/14 - test this
public class SingleWatcher {
    private final static Logger logger = LoggerFactory.getLogger(SingleWatcher.class);

    private final CuratorFramework curator;
    private final ExecutorService executorService;
    private Watcher watcher;
    private CuratorListener listener;

    @Inject
    public SingleWatcher(CuratorFramework curator) {
        this.curator = curator;
        executorService = Executors.newSingleThreadExecutor();
    }

    @VisibleForTesting
    void addCuratorListener() {
        listener = new CuratorListener() {
            @Override
            public void eventReceived(CuratorFramework client, final CuratorEvent event) throws Exception {
                logger.debug("event {}", event);
                if (watcher.getPath().equals(event.getPath())) {
                    executorService.submit(new Runnable() {
                        @Override
                        public void run() {
                            addWatch(watcher.getPath());
                            watcher.callback(event);
                        }
                    });
                }
            }
        };
        curator.getCuratorListenable().addListener(listener);
    }

    public void register(Watcher watcher) {
        this.watcher = watcher;
        addCuratorListener();
        addWatch(watcher.getPath());
    }

    public void unregister() {
        curator.getCuratorListenable().removeListener(listener);
    }

    private void addWatch(String path) {
        try {
            curator.getData().watched().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to start watcher", e);
        }
    }

}
