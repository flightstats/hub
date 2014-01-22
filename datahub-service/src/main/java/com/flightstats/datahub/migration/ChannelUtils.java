package com.flightstats.datahub.migration;

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

/**
 *
 */
//todo - gfm - 1/21/14 - figure out test for this.
public class ChannelUtils {

    public static final int NOT_FOUND = -1;
    private final static Logger logger = LoggerFactory.getLogger(ChannelUtils.class);

    private Client noRedirectsClient;
    private final Client followClient;
    private final ChannelNameExtractor extractor;
    private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();

    @Inject
    public ChannelUtils(@Named("NoRedirects") Client noRedirectsClient, Client followClient, ChannelNameExtractor extractor) {
        this.noRedirectsClient = noRedirectsClient;
        this.followClient = followClient;
        this.extractor = extractor;
    }

    public Optional<Long> getLatestSequence(String channelUrl) {
        if (!channelUrl.endsWith("/")) {
            channelUrl += channelUrl;
        }
        ClientResponse response = noRedirectsClient.resource(channelUrl + "latest")
                .accept(MediaType.WILDCARD_TYPE)
                .get(ClientResponse.class);
        if (response.getStatus() != Response.Status.SEE_OTHER.getStatusCode()) {
            logger.info("latest not found for " + channelUrl + " " + response);
            return Optional.absent();
        }
        String location = response.getLocation().toString();
        String substring = location.substring(channelUrl.length());
        return Optional.of(Long.parseLong(substring));
    }

    public ChannelConfiguration getConfiguration(String channelUrl) throws IOException {
        ClientResponse response = followClient.resource(channelUrl).get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.warn("exiting thread - unable to locate remote channel " + response);
            return null;
        }
        String json = response.getEntity(String.class);
        ChannelConfiguration configuration = ChannelConfiguration.builder()
                .withJson(json)
                .withName(extractor.extractFromChannelUrl(channelUrl))
                .withCreationDate(new Date())
                .build();
        logger.info("found config " + configuration);
        return configuration;
    }

    public Optional<Content> getContent(String channelUrl, long sequence) {
        ClientResponse response = getResponse(channelUrl + sequence);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.info("unable to continue " + response);
            return Optional.absent();
        }
        byte[] data = response.getEntity(byte[].class);
        Optional<String> type = Optional.fromNullable(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        Optional<String> language = Optional.fromNullable(response.getHeaders().getFirst(Headers.LANGUAGE));
        Content content = new Content(type, language, data, getCreationDate(response).getMillis());

        content.setContentKey(new SequenceContentKey(sequence));
        return Optional.of(content);
    }

    public Optional<DateTime> getCreationDate(String channelUrl, long sequence) {
        ClientResponse response = getResponse(channelUrl + sequence);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.info("unable to continue " + response);
            return Optional.absent();
        }

        return Optional.of(getCreationDate(response));
    }

    private ClientResponse getResponse(String url) {
        return noRedirectsClient.resource(url).accept(MediaType.WILDCARD_TYPE).get(ClientResponse.class);
    }

    private DateTime getCreationDate(ClientResponse response) {
        String creationDate = response.getHeaders().getFirst(Headers.CREATION_DATE);
        return dateTimeFormatter.parseDateTime(creationDate);
    }

}
