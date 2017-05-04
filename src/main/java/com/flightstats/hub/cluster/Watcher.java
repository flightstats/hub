package com.flightstats.hub.cluster;

import org.apache.curator.framework.api.CuratorEvent;

public interface Watcher {

    /**
     * The callback will be run in a new thread.  Many applications will want some sort of synchronization.
     */
    void callback(CuratorEvent event);

    /**
     * The unique path to watch.
     */
    String getPath();

    default boolean watchChildren() {
        return false;
    }
}
