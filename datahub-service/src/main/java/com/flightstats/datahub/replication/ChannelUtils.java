package com.flightstats.datahub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.service.Headers;
import com.flightstats.datahub.service.eventing.ChannelNameExtractor;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
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

/**
 *
 */
public class ChannelUtils {

    public static final int NOT_FOUND = -1;
    private final static Logger logger = LoggerFactory.getLogger(ChannelUtils.class);

    private Client noRedirectsClient;
    private final Client followClient;
    private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
    private static ObjectMapper mapper = new ObjectMapper();

    @Inject
    public ChannelUtils(@Named("NoRedirects") Client noRedirectsClient, Client followClient) {
        this.noRedirectsClient = noRedirectsClient;
        this.followClient = followClient;
    }

    public Optional<Long> getLatestSequence(String channelUrl) {
        channelUrl = appendSlash(channelUrl);
        ClientResponse response = noRedirectsClient.resource(channelUrl + "latest")
                .accept(MediaType.WILDCARD_TYPE)
                .head();
        if (response.getStatus() != Response.Status.SEE_OTHER.getStatusCode()) {
            logger.debug("latest not found for " + channelUrl + " " + response);
            return Optional.absent();
        }
        String location = response.getLocation().toString();
        String substring = location.substring(channelUrl.length());
        return Optional.of(Long.parseLong(substring));
    }

    private String appendSlash(String channelUrl) {
        if (!channelUrl.endsWith("/")) {
            channelUrl += "/";
        }
        return channelUrl;
    }

    public Optional<ChannelConfiguration> getConfiguration(String channelUrl) throws IOException {
        ClientResponse response = followClient.resource(channelUrl).get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.warn("exiting thread - unable to locate remote channel " + response);
            return Optional.absent();
        }
        String json = response.getEntity(String.class);
        ChannelConfiguration configuration = ChannelConfiguration.builder()
                .withJson(json)
                .withName(ChannelNameExtractor.extractFromChannelUrl(channelUrl))
                .withCreationDate(new Date())
                .build();
        logger.debug("found config " + configuration);
        return Optional.of(configuration);
    }

    //todo - gfm - 1/25/14 - use compression for content
    public Optional<Content> getContent(String channelUrl, long sequence) {
        channelUrl = appendSlash(channelUrl);
        ClientResponse response = getResponse(channelUrl + sequence);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.debug("unable to get content " + response);
            return Optional.absent();
        }
        Content content = Content.builder()
                .withContentKey(new SequenceContentKey(sequence))
                .withContentType(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE))
                .withContentLanguage(response.getHeaders().getFirst(Headers.LANGUAGE))
                .withData(response.getEntity(byte[].class))
                .withMillis(getCreationDate(response).getMillis())
                .build();

        return Optional.of(content);
    }

    public Optional<DateTime> getCreationDate(String channelUrl, long sequence) {
        channelUrl = appendSlash(channelUrl);
        ClientResponse response = getResponse(channelUrl + sequence);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.debug("unable to get creation date " + response);
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

    private ClientResponse getResponse(String url) {
        /**
         * this uses no redirects because I don't think we want to follow redirects for content,
         * as it could end up in an infinite loop.
         */
        return noRedirectsClient.resource(url).accept(MediaType.WILDCARD_TYPE).get(ClientResponse.class);
    }

    private DateTime getCreationDate(ClientResponse response) {
        String creationDate = response.getHeaders().getFirst(Headers.CREATION_DATE);
        return dateTimeFormatter.parseDateTime(creationDate);
    }

}
