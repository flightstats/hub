package com.flightstats.datahub.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZooKeeperState {
    private final static Logger logger = LoggerFactory.getLogger(ZooKeeperState.class);

    private ConnectionState connectionState = ConnectionState.SUSPENDED;
    private final ConnectionStateListener connectionStateListener;

    public ZooKeeperState() {
        connectionStateListener = new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                connectionState = newState;
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
}
