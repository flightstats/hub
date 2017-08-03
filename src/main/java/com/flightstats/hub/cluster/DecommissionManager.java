package com.flightstats.hub.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface DecommissionManager {
    Logger logger = LoggerFactory.getLogger(DecommissionManager.class);

    default boolean decommission() throws Exception {
        logger.error("default does nothing");
        return true;
    }

    default void commission(String server) throws Exception {
        logger.error("default does nothing");
    }

}
