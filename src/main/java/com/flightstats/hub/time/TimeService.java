package com.flightstats.hub.time;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.config.properties.AppProperties;
import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import javax.inject.Inject;
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
import java.nio.file.InvalidPathException;
import java.nio.file.Paths;

@Singleton
@Slf4j
public class TimeService {

    private final static Client client = RestClient.createClient(1, 5, true, false);
    private final static String randomKey = StringUtils.randomAlphaNumeric(6);
    private boolean isRemote = false;

    private final Cluster cluster;
    private final String remoteFile;
    private final String uriScheme;

    @Inject
    public TimeService(@Named("HubCluster") Cluster cluster,
                       AppProperties appProperties,
                       LocalHostProperties localHostProperties) {
        this.cluster = cluster;

        this.remoteFile = appProperties.getAppRemoteTimeFile();
        this.uriScheme = localHostProperties.getUriScheme();
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
                response = client.resource(uriScheme + server + "/internal/time/millis")
                        .get(ClientResponse.class);
                if (response.getStatus() == 200) {
                    Long millis = Long.parseLong(response.getEntity(String.class));
                    log.trace("using remote time {} from {}", millis, server);
                    return new DateTime(millis, DateTimeZone.UTC);
                }
            } catch (ClientHandlerException e) {
                if (e.getCause() != null && e.getCause() instanceof ConnectException) {
                    log.warn("connection exception {}", server);
                } else {
                    log.warn("unable to get time for server {}", server, e);
                }
            } catch (Exception e) {
                log.warn("unable to get time for server {}", server, e);
            } finally {
                HubUtils.close(response);
            }
        }
        return null;
    }

    private void deleteFile() {
        try {
            // Validate and sanitize the remoteFile path
            File file = Paths.get(remoteFile).normalize().toFile();

            if (file.exists()) {
                file.delete();
            }
            log.info("deleted remote file {}; will use local ", remoteFile);
        } catch (InvalidPathException e) {
            log.error("invalid path for remote file", e);
            throw new RuntimeException("invalid path for remoteFile " + remoteFile);
        }
    }

    private void createFile() {
        try {
            File file = new File(remoteFile);
            FileUtils.write(file, "true", StandardCharsets.UTF_8);
            log.info("wrote remote file {}", remoteFile);
        } catch (IOException e) {
            log.error("unable to write remote file", e);
            throw new RuntimeException("unable to write remoteFile " + remoteFile);
        }
    }

    public boolean isRemote() {
        return isRemote;
    }

    public void setRemote(boolean remote) {
        isRemote = remote;
        log.debug("remote {}", remote);
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
