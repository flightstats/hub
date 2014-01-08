package com.flightstats.datahub.dao.s3;

import com.flightstats.datahub.dao.TimeIndex;
import com.flightstats.datahub.util.TimeProvider;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class TimeIndexCoordinator implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(TimeIndexCoordinator.class);

    private final CuratorFramework curator;
    private final TimeIndexDao timeIndexDao;
    private final TimeProvider timeProvider;

    @Inject
    public TimeIndexCoordinator(CuratorFramework curator, TimeIndexDao timeIndexDao, TimeProvider timeProvider) {
        this.curator = curator;
        this.timeIndexDao = timeIndexDao;
        this.timeProvider = timeProvider;
    }

    @Override
    public void run() {
        try {
            List<String> channels = curator.getChildren().forPath(TimeIndex.getPath());
            logger.info("found " + channels.size() + " channels");
            Collections.shuffle(channels);
            for (String channel : channels) {
                new TimeIndexProcessor(curator, timeIndexDao, timeProvider).process(channel);
            }
        } catch (Exception e) {
            logger.info("unable to process", e);
        }
    }
}
