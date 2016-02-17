package com.flightstats.hub.time;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.spoke.RemoteSpokeStore;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;

@Singleton
public class TimeService {

    private final static Logger logger = LoggerFactory.getLogger(TimeService.class);

    private final String externalFile = HubProperties.getProperty("app.externalFile", "/home/hub/externalTime");
    private final static Client client = RestClient.createClient(1, 5, true);

    @Inject
    @Named("HubCuratorCluster")
    private CuratorCluster cluster;

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

    public DateTime getNow() {
        if (!isExternal) {
            return TimeUtil.now();
        }
        DateTime millis = getRemoteNow();
        if (millis != null) {
            return millis;
        }
        logger.warn("unable to get external time, using local!");
        return TimeUtil.now();
    }

    public DateTime getRemoteNow() {
        for (String server : cluster.getRandomRemoteServers()) {
            ClientResponse response = null;
            try {
                response = client.resource(HubHost.getScheme() + server + "/internal/time/millis")
                        .get(ClientResponse.class);
                if (response.getStatus() == 200) {
                    Long millis = Long.parseLong(response.getEntity(String.class));
                    logger.trace("using remote time {} from {}", millis, server);
                    return new DateTime(millis, DateTimeZone.UTC);
                }
            } catch (ClientHandlerException e) {
                if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                    logger.warn("connection exception " + server);
                } else {
                    logger.warn("unable to get time " + server, e);
                }
            } catch (Exception e) {
                logger.warn("unable to get time " + server, e);
            } finally {
                RemoteSpokeStore.close(response);
            }
        }
        return null;
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
