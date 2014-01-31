package com.flightstats.hub.cluster;

import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ZooKeeperState {
    private final static Logger logger = LoggerFactory.getLogger(ZooKeeperState.class);

    private ConnectionState connectionState = ConnectionState.SUSPENDED;
    private final ConnectionStateListener connectionStateListener;
    private final AtomicBoolean stopWorking = new AtomicBoolean(false);

    @Inject
    public ZooKeeperState() {
        connectionStateListener = new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                connectionState = newState;
                stopWorking.set(!isHealthy());
                logger.info("state change from " + connectionState + " to " + newState);
            }
        };
    }

    public  ConnectionStateListener getStateListener() {
        return connectionStateListener;
    }

    public boolean isHealthy() {
        switch (connectionState) {
            case CONNECTED:
            case RECONNECTED:
                return true;
        }
        return false;
    }

    public boolean shouldStopWorking() {
        return stopWorking.get();
    }
}
