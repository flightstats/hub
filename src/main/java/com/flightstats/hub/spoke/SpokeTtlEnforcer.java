package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class SpokeTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeTtlEnforcer.class);
    private final String storagePath;
    private final int ttlMinutes;

    @Inject
    public SpokeTtlEnforcer(@Named("spoke.path") String storagePath,
                            @Named("spoke.ttlMinutes") int ttlMinutes) {
        this.storagePath = storagePath;
        this.ttlMinutes = ttlMinutes;
        HubServices.register(new SpokeTtlEnforcerService());
    }

    public void run() {
        try {
            File root = new File(storagePath);
            File[] files = root.listFiles();
            for (File file : files) {
                if (file.isDirectory()) {
                    processChannel(file);
                }
            }
        } catch (Exception e) {
            logger.warn("unable to enforce ttl", e);
        }
    }

    private void processChannel(File file) {
        try {
            DateTime now = TimeUtil.now();
            DateTime cursor = now.minusMinutes(ttlMinutes);
            for (int i = 1; i <= 60; i++) {
                cursor = cursor.minusMinutes(1);
                delete(file, TimeUtil.minutes(cursor));
            }
            for (int i = 1; i <= 6; i++) {
                delete(file, TimeUtil.hours(cursor));
                cursor = cursor.minusHours(1);
            }
        } catch (Exception e) {
            logger.warn("unable to delete " + file, e);
        }
    }

    private void delete(File file, String datePath) {
        String path = file.toString() + "/" + datePath;
        FileUtils.deleteQuietly(new File(path));
    }

    private class SpokeTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(1, 5, TimeUnit.MINUTES);
        }

    }

}
