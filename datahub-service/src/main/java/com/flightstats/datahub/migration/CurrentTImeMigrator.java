package com.flightstats.datahub.migration;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.service.Headers;
import com.flightstats.datahub.util.ClientCreator;
import com.flightstats.datahub.util.Sleeper;
import com.google.common.base.Optional;
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

public class CurrentTimeMigrator implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(CurrentTimeMigrator.class);
    private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
    private static final int NOT_FOUND = -1;

    private final ChannelService channelService;
    private final String host;
    private final String channel;
    private Client client;
    private String channelUrl;
    private ChannelConfiguration configuration;

    public CurrentTimeMigrator(ChannelService channelService, String host, String channel) {
        this.channelService = channelService;
        this.host = host;
        this.channel = channel;
        client = ClientCreator.cached();
        channelUrl = "http://" + host + "/channel/" + channel + "/";
    }

    @Override
    public void run() {
        try {
            if (initialize()) return;
        } catch (IOException e) {
            logger.warn("unable to parse json for " + channelUrl, e);
            return;
        }
        while (migrate()) {
            Sleeper.sleep(5000);
        }
    }

    private boolean initialize() throws IOException {
        //todo - gfm - 1/20/14 - this url is clumsy
        ClientResponse response = client.resource("http://" + host + "/channel/" + channel).get(ClientResponse.class);
        if (response.getStatus() >= 400) {
            logger.warn("exiting thread - unable to locate remote channel " + response);
            return true;
        }
        String json = response.getEntity(String.class);
        configuration = ChannelConfiguration.builder()
                .withJson(json)
                .withName(channel)
                .withCreationDate(new Date())
                .build();
        logger.info("found config " + configuration);
        //todo - gfm - 1/20/14 - this should verify the TTL hasn't changed
        if (!channelService.channelExists(channel)) {
            channelService.createChannel(configuration);
        }
        return false;
    }

    private boolean migrate() {

        long sequence = getStartingSequence();
        if (sequence == NOT_FOUND) {
            return false;
        }
        logger.info("starting " + channelUrl + " migration at " + sequence);
        //todo - gfm - 1/20/14 - uncomment
        /*Content content = getContent(sequence);
        while (content != null) {
            channelService.insert(channel, content);
            sequence++;
            content = getContent(sequence);
        }

        return true;*/
        return false;
    }

    private Content getContent(long sequence) {
        ClientResponse response = getResponse(sequence);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.info("unable to continue " + response);
            return null;
        }
        byte[] data = response.getEntity(byte[].class);
        Optional<String> type = Optional.fromNullable(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        Optional<String> language = Optional.fromNullable(response.getHeaders().getFirst(Headers.LANGUAGE));
        Content content = new Content(type, language, data, getCreationDate(response).getMillis());

        content.setContentKey(new SequenceContentKey(sequence));
        return content;
    }

    private DateTime getCreationDate(ClientResponse response) {
        String creationDate = response.getHeaders().getFirst(Headers.CREATION_DATE);
        return dateTimeFormatter.parseDateTime(creationDate);
    }

    private ClientResponse getResponse(long sequence) {
        return client.resource(channelUrl + sequence).accept(MediaType.WILDCARD_TYPE).get(ClientResponse.class);
    }

    private long getStartingSequence() {
        Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(channel);
        if (lastUpdatedKey.isPresent()) {
            SequenceContentKey contentKey = (SequenceContentKey) lastUpdatedKey.get();
            if (contentKey.getSequence() == SequenceContentKey.START_VALUE) {
                return searchForStartingKey();
            }
            return contentKey.getSequence() + 1;
        }
        logger.warn("problem getting starting sequence " + channelUrl);
        return NOT_FOUND;
    }

    private long searchForStartingKey() {
        //this may not play well with discontinuous sequences
        logger.info("searching the key space for " + channelUrl);

        long high = getLatest();
        if (high == NOT_FOUND) {
            return SequenceContentKey.START_VALUE + 1;
        }
        //todo - gfm - 1/20/14 - would be useful to pull this out into something that can be rigorously tested
        long low = SequenceContentKey.START_VALUE;
        long lastExists = high;
        while (low <= high && (high - low) > 1) {
            long middle = low + (high - low) / 2;
            //do get on middle
            if (existsAndNotYetExpired(middle)) {
                high = middle - 1;
                lastExists = middle;
            } else {
                low = middle;
            }
            logger.info("low=" + low + " high=" + high + " middle=" + middle);
        }
        logger.info("returning starting key " + lastExists);
        return lastExists;
    }

    /**
     * We want to return a starting id that exists, and isn't going to be expired immediately.
     */
    private boolean existsAndNotYetExpired(long id) {
        ClientResponse response = getResponse(id);
        int status = response.getStatus();
        logger.info("status " + channelUrl + " id=" + id + " status=" + status);
        if (status >= 400) {
            return false;
        }
        if (configuration.getTtlMillis() == null) {
            return true;
        }
        long ttlMillis = configuration.getTtlMillis();
        DateTime creationDate = getCreationDate(response);
        DateTime tenMinuteOffset = new DateTime().minusMillis((int) ttlMillis).plusMinutes(10);
        return creationDate.isAfter(tenMinuteOffset);
    }

    private long getLatest() {
        ClientResponse response = client.resource(channelUrl + "latest").accept(MediaType.WILDCARD_TYPE).get(ClientResponse.class);
        if (response.getStatus() != Response.Status.SEE_OTHER.getStatusCode()) {
            logger.info("latest not found for " + channelUrl + " " + response);
            return NOT_FOUND;
        }
        String location = response.getLocation().toString();
        String substring = location.substring(channelUrl.length());
        return Long.parseLong(substring);
    }
}
