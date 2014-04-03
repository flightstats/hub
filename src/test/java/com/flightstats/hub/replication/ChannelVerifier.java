package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 *
 */
public class ChannelVerifier implements Callable<VerifierResult> {

    private final static Logger logger = LoggerFactory.getLogger(ChannelVerifier.class);

    private JsonNode channelStatus;
    private String replicationUri;
    private final int frequencyHours;
    private final int verificationPercent;
    private final SequenceFinder sequenceFinder;
    private final ChannelUtils channelUtils;

    public ChannelVerifier(JsonNode channelStatus, String replicationUri, int frequencyHours, int verificationPercent,
                           SequenceFinder sequenceFinder, ChannelUtils channelUtils) {
        this.channelStatus = channelStatus;
        this.replicationUri = replicationUri;
        this.frequencyHours = frequencyHours;
        this.verificationPercent = verificationPercent;
        this.sequenceFinder = sequenceFinder;
        this.channelUtils = channelUtils;
    }

    @Override
    public VerifierResult call() throws Exception {
        VerifierResult result = new VerifierResult();
        long sequence = getStartSequence();
        int sourceLatest = getSourceLatest();
        String channelUri = replicationUri + "channel/" + getName() + "/";
        logger.info("verifying {} from {} to {}, {} items", channelUri, sequence, sourceLatest, sourceLatest - sequence);
        while (sequence <= sourceLatest) {
            Optional<Content> content = channelUtils.getContent(channelUri, sequence);
            result.incrementSequencesChecked();
            //todo - gfm - 4/2/14 - also check payload
            if (!content.isPresent()) {
                result.addMissingSequence(String.valueOf(sequence));
            }
            sequence+=100;
        }
        logger.info("completed " + channelUri + " " + result);
        return result;
    }

    private long getStartSequence() {
        Channel channel = new Channel(getName(), getSourceUrl());
        channel.setConfiguration(ChannelConfiguration.builder().withTtlDays(1).build());
        int start = Math.max(999, getSourceLatest() - 50000);
        long minutes = TimeUnit.HOURS.toMinutes(frequencyHours) + 10;
        return sequenceFinder.searchForLastUpdated(channel, start, minutes, TimeUnit.MINUTES) + 1;
    }

    private String getSourceUrl() {
        return channelStatus.get("url").asText();
    }

    private String getName() {
        return channelStatus.get("name").asText();
    }

    private int getSourceLatest() {
        return channelStatus.get("sourceLatest").asInt();
    }

}
