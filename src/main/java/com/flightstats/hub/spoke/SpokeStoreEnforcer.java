package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Arrays;

import static com.flightstats.hub.dao.file.FileUtil.createDirectory;
import static com.flightstats.hub.dao.file.FileUtil.directoryExists;
import static com.flightstats.hub.dao.file.FileUtil.move;

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
            logger.info("verifying the spoke store exists");
            if (directoryExists(spokePath)) {
                logger.info("spoke store found {}", spokePath);
            } else {
                logger.info("looking for spoke stores at previous paths {}", previousSpokePaths);
                createDirectory(spokePath);
                Arrays.stream(previousSpokePaths.split(",")).forEach(this::migrate);
            }
        }

        private void migrate(String previousPath) {
            if (directoryExists(previousPath)) {
                long start = System.currentTimeMillis();
                try {
                    logger.info("moving files from {} to {}", previousPath, spokePath);
                    move(previousPath, spokePath);
                } catch (IOException e) {
                    logger.info("ignoring spoke store at {}", previousPath);
                } finally {
                    logger.info("move completed in {} ms", (System.currentTimeMillis() - start));
                }
            } else {
                logger.info("no data found {}", previousPath);
            }
        }

        @Override
        protected void shutDown() throws Exception {
            // do nothing
        }
    }
}
