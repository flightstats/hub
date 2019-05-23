package com.flightstats.hub.cluster;

public interface DecommissionManager {

    default boolean decommission() throws Exception {
        return true;
    }

    default void recommission(String server) throws Exception {
    }

}
