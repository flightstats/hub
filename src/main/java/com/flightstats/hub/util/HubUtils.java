package com.flightstats.hub.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.replication.Channel;
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
import java.util.HashSet;
import java.util.Set;

public class HubUtils {

    public static final int NOT_FOUND = -1;
    private final static Logger logger = LoggerFactory.getLogger(HubUtils.class);
    private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
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

    public Optional<Content> getContent(String contentUrl) {
        ClientResponse response = getResponse(contentUrl);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.info("unable to get content " + response);
            return Optional.absent();
        }
        Content content = Content.builder()
                .withContentKey(ContentKey.fromFullUrl(contentUrl).get())
                .withContentType(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .withContentLanguage(response.getHeaders().getFirst(Headers.LANGUAGE))
                .withData(response.getEntity(byte[].class))
                .build();

        return Optional.of(content);
    }

    public Optional<DateTime> getCreationDate(String channelUrl, long sequence) {
        ClientResponse response = getResponse(appendSlash(channelUrl) + sequence);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.info("unable to get creation date " + response);
            return Optional.absent();
        }

        return Optional.of(getCreationDate(response));
    }

    public Set<Channel> getChannels(String url) {
        Set<Channel> channels = new HashSet<>();
        try {
            ClientResponse response = followClient.resource(url).get(ClientResponse.class);
            if (response.getStatus() >= 400) {
                logger.warn("unable to get channels " + response);
                return channels;
            }
            JsonNode rootNode = mapper.readTree(response.getEntity(String.class));
            JsonNode channelsNode = rootNode.get("_links").get("channels");
            for (JsonNode channel : channelsNode) {
                channels.add(new Channel(channel.get("name").asText(), channel.get("href").asText()));
            }
        } catch (Exception e) {
            logger.warn("unable to get channels " + url + " " + e.getMessage());
            logger.debug("unable to get channels " + url, e);
        }
        return channels;
    }

    public void startGroupCallback(String groupName, String callbackUrl, String sourceChannel) {
        String sourceRoot = StringUtils.substringBefore(sourceChannel, "/channel/");
        ObjectNode payload = mapper.createObjectNode();
        payload.put("callbackUrl", callbackUrl);
        payload.put("channelUrl", sourceChannel);
        String groupUrl = sourceRoot + "/group/" + groupName;
        logger.info("starting {} with {}", groupUrl, payload);
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .put(ClientResponse.class, payload.toString());
        logger.info("start group response {}", response);
    }

    public void stopGroupCallback(String groupName, String sourceChannel) {
        String sourceRoot = StringUtils.substringBefore(sourceChannel, "/channel/");
        String groupUrl = sourceRoot + "/group/" + groupName;
        logger.info("delete {} ", groupUrl);
        ClientResponse response = followClient.resource(groupUrl)
                .accept(MediaType.APPLICATION_JSON)
                .type(MediaType.APPLICATION_JSON)
                .delete(ClientResponse.class);
        logger.info("stop group response {}", response);

    }

    private ClientResponse getResponse(String url) {
        return followClient.resource(url)
                .accept(MediaType.WILDCARD_TYPE)
                .header(HttpHeaders.ACCEPT_ENCODING, "gzip")
                .get(ClientResponse.class);
    }

    private DateTime getCreationDate(ClientResponse response) {
        String creationDate = response.getHeaders().getFirst(Headers.CREATION_DATE);
        return dateTimeFormatter.parseDateTime(creationDate);
    }

}
