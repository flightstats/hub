package com.flightstats.hub.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.group.Group;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.rest.Headers;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Date;

public class HubUtils {

    public static final int NOT_FOUND = -1;
    private final static Logger logger = LoggerFactory.getLogger(HubUtils.class);
    public static final DateTimeFormatter FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();
    private static ObjectMapper mapper = new ObjectMapper();
    private final Client noRedirectsClient;
    private final Client followClient;

    public enum Version {
        V2,
        Unknown
    }

    @Inject
    public HubUtils(@Named("NoRedirects") Client noRedirectsClient, Client followClient) {
        this.noRedirectsClient = noRedirectsClient;
        this.followClient = followClient;
    }

    public Optional<String> getLatest(String channelUrl) {
        channelUrl = appendSlash(channelUrl);
        ClientResponse response = noRedirectsClient.resource(channelUrl + "latest")
                .accept(MediaType.WILDCARD_TYPE)
                .head();
        if (response.getStatus() != Response.Status.SEE_OTHER.getStatusCode()) {
            logger.info("latest not found for " + channelUrl + " " + response);
            return Optional.absent();
        }
        return Optional.of(response.getLocation().toString());
    }

    private String appendSlash(String channelUrl) {
        if (!channelUrl.endsWith("/")) {
            channelUrl += "/";
        }
        return channelUrl;
    }

    public Optional<ChannelConfig> getConfiguration(String channelUrl) throws IOException {
        ClientResponse response = followClient.resource(channelUrl).get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.info("unable to locate remote channel " + response);
            return Optional.absent();
        }
        String json = response.getEntity(String.class);
        ChannelConfig configuration = ChannelConfig.builder()
                .withChannelConfiguration(ChannelConfig.fromJson(json))
                .withName(ChannelNameUtils.extractFromChannelUrl(channelUrl))
                .withCreationDate(new Date())
                .build();
        logger.debug("found config " + configuration);
        return Optional.of(configuration);
    }

    public Optional<DateTime> getCreationDate(String channelUrl, long sequence) {
        ClientResponse response = getResponse(appendSlash(channelUrl) + sequence);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.info("unable to get creation date " + response);
            return Optional.absent();
        }

        return Optional.of(getCreationDate(response));
    }

    public Optional<Group> getGroupCallback(String groupName, String sourceChannel) {
        String groupUrl = getSourceUrl(sourceChannel) + "/group/" + groupName;
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .get(ClientResponse.class);
        logger.info("get group response {}", response);
        if (response.getStatus() < 400) {
            return Optional.of(Group.fromJson(response.getEntity(String.class)));
        }
        return Optional.absent();
    }

    public void startGroupCallback(Group group) {
        String groupUrl = getSourceUrl(group.getChannelUrl()) + "/group/" + group.getName();
        String json = group.toJson();
        logger.info("starting {} with {}", groupUrl, json);
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .put(ClientResponse.class, json);
        logger.info("start group response {}", response);
    }

    public void stopGroupCallback(String groupName, String sourceChannel) {
        String groupUrl = getSourceUrl(sourceChannel) + "/group/" + groupName;
        logger.info("stopping {} ", groupUrl);
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .delete(ClientResponse.class);
        logger.debug("stop group response {}", response);

    }

    private String getSourceUrl(String sourceChannel) {
        return StringUtils.substringBefore(sourceChannel, "/channel/");
    }

    public ClientResponse getResponse(String url) {
        return followClient.resource(url)
                .accept(MediaType.WILDCARD_TYPE)
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .get(ClientResponse.class);
    }

    private DateTime getCreationDate(ClientResponse response) {
        String creationDate = response.getHeaders().getFirst(Headers.CREATION_DATE);
        return FORMATTER.parseDateTime(creationDate);
    }

}
