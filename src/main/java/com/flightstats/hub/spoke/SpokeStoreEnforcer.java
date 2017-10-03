package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.file.FileUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;

@Singleton
public class SpokeStoreEnforcer {

    private final static Logger logger = LoggerFactory.getLogger(SpokeStoreEnforcer.class);
    private final static String spokePath = HubProperties.getSpokePath();
    private final static String previousSpokePaths = HubProperties.getProperty("spoke.previousPaths", "");

    @Inject
    public SpokeStoreEnforcer() {
        if (HubProperties.getProperty("spoke.enforceStore", true)) {
            HubServices.register(new SpokeStoreEnforcerService());
        }
    }

    private class SpokeStoreEnforcerService extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            if (!FileUtil.directoryExists(spokePath)) {
                logger.info("creating spoke store {}", spokePath);
                FileUtil.createDirectory(spokePath);
            }

            if (!StringUtils.isEmpty(previousSpokePaths)) {
                logger.info("consolidating spoke data from {}", previousSpokePaths);
                Arrays.stream(previousSpokePaths.split(","))
                        .map(String::trim)
                        .forEach(this::migrate);
            }
        }

        private void migrate(String previousPath) {
            if (FileUtil.directoryExists(previousPath)) {
                try {
                    long start = System.currentTimeMillis();
                    FileUtil.mergeDirectories(previousPath, spokePath);
                    FileUtil.delete(previousPath);
                    logger.info("move completed in {} ms", (System.currentTimeMillis() - start));
                } catch (IOException e) {
                    logger.info("ignoring spoke store at {}", previousPath, e);
                }
            } else {
                logger.info("no data found at {}", previousPath);
            }
        }

        @Override
        protected void shutDown() throws Exception {
            // do nothing
        }
    }
}
