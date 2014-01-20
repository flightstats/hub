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

public class CurrentTimeMigrator implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(CurrentTimeMigrator.class);
    private static final DateTimeFormatter dateTimeFormatter = ISODateTimeFormat.dateTime().withZoneUTC();
    private final ChannelService channelService;
    private final String channel;
    private Client client;
    private String channelUrl;

    public CurrentTimeMigrator(ChannelService channelService, String host, String channel) {
        this.channelService = channelService;
        this.channel = channel;
        client = ClientCreator.cached();
        channelUrl = "http://" + host + "/channel/" + channel + "/";
    }

    @Override
    public void run() {
        while (migrate()) {
            Sleeper.sleep(5000);
        }
    }

    private boolean migrate() {
        if (!channelService.channelExists(channel)) {
            //todo - gfm - 1/19/14 - get config from target host
            channelService.createChannel(ChannelConfiguration.builder().withName(channel).build());
        }
        long sequence = getStartingSequence();
        if (sequence == -1) {
            return false;
        }
        logger.info("starting " + channelUrl + " migration at " + sequence);
        Content content = getContent(sequence);
        while (content != null) {
            channelService.insert(channel, content);
            sequence++;
            content = getContent(sequence);
        }

        return true;
    }

    private Content getContent(long sequence) {
        ClientResponse response = client.resource(channelUrl + sequence).accept(MediaType.WILDCARD_TYPE).get(ClientResponse.class);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            logger.info("unable to continue " + response);
            return null;
        }
        byte[] data = response.getEntity(byte[].class);
        Optional<String> type = Optional.fromNullable(response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        Optional<String> language = Optional.fromNullable(response.getHeaders().getFirst(Headers.LANGUAGE));
        String creationDate = response.getHeaders().getFirst(Headers.CREATION_DATE);
        DateTime dateTime = dateTimeFormatter.parseDateTime(creationDate);
        Content content = new Content(type, language, data, dateTime.getMillis());

        content.setContentKey(new SequenceContentKey(sequence));
        return content;
    }

    private long getStartingSequence() {
        Optional<ContentKey> lastUpdatedKey = channelService.findLastUpdatedKey(channel);
        if (lastUpdatedKey.isPresent()) {
            SequenceContentKey contentKey = (SequenceContentKey) lastUpdatedKey.get();
            return contentKey.getSequence() + 1;
        }
        logger.warn("problem getting starting sequence " + channelUrl);
        return -1;
        /*ClientResponse response = client.resource(channelUrl + "latest").accept(MediaType.WILDCARD_TYPE).get(ClientResponse.class);
        if (response.getStatus() != Response.Status.SEE_OTHER.getStatusCode()) {
            logger.info("latest not found for " + channelUrl + " " + response);
            return -1;
        }
        String location = response.getLocation().toString();
        String substring = location.substring(channelUrl.length());
        //todo - gfm - 1/19/14 - go back in time to the beginning of the minute.
        return Long.parseLong(substring);*/
    }

        /*public static void main(String[] args) throws ConstraintException {
        Properties properties = new Properties();
        properties.put("backing.store", "aws");
        properties.put("dynamo.endpoint", "localhost:8000");
        properties.put("aws.protocol", "HTTP");
        properties.put("aws.environment", "test");
        properties.put("dynamo.table_creation_wait_minutes", "5");
        properties.put("aws.credentials", "default");
        properties.put("hazelcast.conf.xml", "");
        properties.put("zookeeper.connection", "localhost:2181");
        properties.put("zookeeper.cfg", "singleNode");
        DataHubMain.startZookeeper(properties);
        Injector injector = GuiceContextListenerFactory.construct(properties).getInjector();
        ChannelService service = injector.getInstance(ChannelService.class);
        CurrentTimeMigrator migrator = new CurrentTimeMigrator(service);
        migrator.migrate("positionAsdiRaw", "http://datahub.svc.prod/");

    }*/
}
