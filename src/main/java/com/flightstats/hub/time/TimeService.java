package com.flightstats.hub.time;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.io.File;
import java.io.IOException;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;

@Singleton
@Slf4j
public class TimeService {

    private final static Client client = RestClient.createClient(1, 5, true, false);
    private final static String randomKey = StringUtils.randomAlphaNumeric(6);
    private boolean isRemote = false;

    private final Cluster cluster;
    private final String remoteFile;

    @Inject
    public TimeService(@Named("HubCluster") Cluster cluster, AppProperties appProperties) {
        this.cluster = cluster;

        this.remoteFile = appProperties.getAppRemoteTimeFile();
        HubServices.register(new TimeServiceRegister());
    }

    public DateTime getNow() {
        if (!isRemote) {
            return TimeUtil.now();
        }
        DateTime millis = getRemoteNow();
        if (millis != null) {
            return millis;
        }
        log.warn("unable to get external time, using local!");
        return TimeUtil.now();
    }

    DateTime getRemoteNow() {
        for (String server : cluster.getRemoteServers(randomKey)) {
            ClientResponse response = null;
            try {
                response = client.resource(HubHost.getScheme() + server + "/internal/time/millis")
                        .get(ClientResponse.class);
                if (response.getStatus() == 200) {
                    Long millis = Long.parseLong(response.getEntity(String.class));
                    log.trace("using remote time {} from {}", millis, server);
                    return new DateTime(millis, DateTimeZone.UTC);
                }
            } catch (ClientHandlerException e) {
                if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                    log.warn("connection exception " + server);
                } else {
                    log.warn("unable to get time " + server, e);
                }
            } catch (Exception e) {
                log.warn("unable to get time " + server, e);
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
        log.info("deleted file " + remoteFile);
    }

    private void createFile() {
        try {
            File file = new File(remoteFile);
            FileUtils.write(file, "true", StandardCharsets.UTF_8);
            log.info("wrote file " + remoteFile);
        } catch (IOException e) {
            log.warn("unable to write file", e);
            throw new RuntimeException("unable to write remoteFile " + remoteFile);
        }
    }

    public boolean isRemote() {
        return isRemote;
    }

    public void setRemote(boolean remote) {
        isRemote = remote;
        log.info("remote {}", remote);
        if (isRemote) {
            createFile();
        } else {
            deleteFile();
        }
    }

    private class TimeServiceRegister extends AbstractIdleService {

        @Override
        protected void startUp() throws Exception {
            File file = new File(remoteFile);
            isRemote = file.exists();
            log.info("calibrating state {} for {}", isRemote, remoteFile);
        }

        @Override
        protected void shutDown() throws Exception {
            //do anything?
        }
    }
}
