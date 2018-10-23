package com.flightstats.hub.time;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

@Singleton
public class TimeService {

    private final static Logger logger = LoggerFactory.getLogger(TimeService.class);

    private final static Client client = RestClient.createClient(1, 5, true, false);
    private final static String randomKey = StringUtils.randomAlphaNumeric(6);

    private final CuratorCluster hubCluster;
    private final String remoteFile;

    private boolean isRemote = false;

    @Inject
    public TimeService(@Named("HubCluster") CuratorCluster hubCluster, HubProperties hubProperties) {
        this.hubCluster = hubCluster;
        this.remoteFile = hubProperties.getProperty("app.remoteTimeFile", "/home/hub/remoteTime");

        HubServices.register(new TimeServiceRegister());
    }

    public void setRemote(boolean remote) {
        isRemote = remote;
        logger.info("remote {}", remote);
        if (isRemote) {
            createFile();
        } else {
            deleteFile();
        }
    }

    public DateTime getNow() {
        if (!isRemote) {
            return TimeUtil.now();
        }
        DateTime millis = getRemoteNow();
        if (millis != null) {
            return millis;
        }
        logger.warn("unable to get external time, using local!");
        return TimeUtil.now();
    }

    DateTime getRemoteNow() {
        for (String server : hubCluster.getRemoteServers(randomKey)) {
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
                HubUtils.close(response);
            }
        }
        return null;
    }

    private void deleteFile() {
        File file = new File(remoteFile);
        if (file.exists()) {
            file.delete();
        }
        logger.info("deleted file " + remoteFile);
    }

    private void createFile() {
        try {
            File file = new File(remoteFile);
            FileUtils.write(file, "true", StandardCharsets.UTF_8);
            logger.info("wrote file " + remoteFile);
        } catch (IOException e) {
            logger.warn("unable to write file", e);
            throw new RuntimeException("unable to write remoteFile " + remoteFile);
        }
    }

    public boolean isRemote() {
        return isRemote;
    }

    private class TimeServiceRegister extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            File file = new File(remoteFile);
            isRemote = file.exists();
            logger.info("calibrating state {} for {}", isRemote, remoteFile);
        }

        @Override
        protected void shutDown() throws Exception {
            //do anything?
        }
    }
}
