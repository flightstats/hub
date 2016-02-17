package com.flightstats.hub.time;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

@Singleton
public class TimeService {

    private final static Logger logger = LoggerFactory.getLogger(TimeService.class);

    private final String externalFile = HubProperties.getProperty("app.externalFile", "/home/hub/externalTime");

    private boolean isExternal = false;

    public TimeService() {
        HubServices.register(new TimeServiceRegister());
    }

    public void setExternal(boolean external) {
        isExternal = external;
        logger.info("external {}", external);
        if (isExternal) {
            createFile();
        } else {
            deleteFile();
        }
    }

    private void deleteFile() {
        File file = new File(externalFile);
        if (file.exists()) {
            file.delete();
        }
        logger.info("deleted file " + externalFile);
    }

    private void createFile() {
        try {
            File file = new File(externalFile);
            FileUtils.write(file, "true");
            logger.info("wrote file " + externalFile);
        } catch (IOException e) {
            logger.warn("unable to write file", e);
            throw new RuntimeException("unable to write externalFile " + externalFile);
        }
    }

    public boolean isExternal() {
        return isExternal;
    }

    private class TimeServiceRegister extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            File file = new File(externalFile);
            isExternal = file.exists();
            logger.info("calibrating state {} for {}", isExternal, externalFile);
        }

        @Override
        protected void shutDown() throws Exception {
            //do anything?
        }
    }
}
