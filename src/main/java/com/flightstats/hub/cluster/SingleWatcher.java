package com.flightstats.hub.cluster;

import com.flightstats.hub.util.RuntimeInterruptedException;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * SingleWatcher asynchronously calls the Watcher in a single thread.
 */
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
        listener = (client, event) -> {
            if (watcher.getPath().equals(event.getPath())) {
                logger.debug("event path {}", event.getPath());
                addWatch(watcher.getPath());
                watcher.callback(event);
            }
        };
        curator.getCuratorListenable().addListener(listener, executorService);
    }

    public void register(Watcher watcher) {
        this.watcher = watcher;
        addCuratorListener();
        addWatch(watcher.getPath());
    }

    public void unregister() {
        curator.getCuratorListenable().removeListener(listener);
        executorService.shutdown();
        try {
            executorService.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    private void addWatch(String path) {
        try {
            curator.getData().watched().forPath(path);
        } catch (InterruptedException ie) {
            logger.info("interrupted on path " + path);
            throw new RuntimeInterruptedException(ie);
        } catch (Exception e) {
            logger.warn("unable to start watcher", e);
        }
    }

}
