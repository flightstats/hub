package com.flightstats.hub.cluster;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

@Singleton
public class ZooKeeperState {
    private final static Logger logger = LoggerFactory.getLogger(ZooKeeperState.class);
    private final ConnectionStateListener connectionStateListener;
    private final AtomicBoolean stopWorking = new AtomicBoolean(false);
    private ConnectionState connectionState = ConnectionState.SUSPENDED;

    @Inject
    public ZooKeeperState() {
        connectionStateListener = (client, newState) -> {
            ConnectionState oldState = connectionState;
            connectionState = newState;
            boolean healthy = isHealthy();
            stopWorking.set(!healthy);
            logger.info("state change from " + oldState + " to " + newState + " healthy " + healthy);
        };
    }

    public ConnectionStateListener getStateListener() {
        return connectionStateListener;
    }

    private boolean isHealthy() {
        switch (connectionState) {
            case CONNECTED:
            case RECONNECTED:
                return true;
        }
        return false;
    }

    boolean shouldStopWorking() {
        return stopWorking.get();
    }

    boolean shouldKeepWorking() {
        return !shouldStopWorking();
    }
}
