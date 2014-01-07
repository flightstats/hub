package com.flightstats.datahub.dao.s3;

import com.flightstats.datahub.dao.TimeIndex;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class TimeIndexCoordinator {
    private final static Logger logger = LoggerFactory.getLogger(TimeIndexCoordinator.class);

    private final CuratorFramework curator;

    @Inject
    public TimeIndexCoordinator(CuratorFramework curator) {
        this.curator = curator;
    }

    /**
     * todo - gfm - 1/6/14 - this should run at a fixed interval
     *
     */


    public void process() {
        try {
            logger.info("running");
            List<String> channels = curator.getChildren().forPath(TimeIndex.getPath());
            logger.info("found " + channels.size());
            Collections.shuffle(channels);
            for (String channel : channels) {
                //todo - gfm - 1/6/14 -
                //new TimeIndexProcessor(curator, channel).process();
            }
            logger.info("completed");
        } catch (Exception e) {
            logger.info("unable to process", e);
        }
    }



}
