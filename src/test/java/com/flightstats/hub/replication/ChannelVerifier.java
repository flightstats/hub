package com.flightstats.hub.replication;

import com.fasterxml.jackson.databind.JsonNode;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Random;
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
    private final int payloadPercent;
    private final SequenceFinder sequenceFinder;
    private final ChannelUtils channelUtils;
    private final Random random = new Random();

    public ChannelVerifier(JsonNode channelStatus, String replicationUri, int frequencyHours, int payloadPercent,
                           SequenceFinder sequenceFinder, ChannelUtils channelUtils) {
        this.channelStatus = channelStatus;
        this.replicationUri = replicationUri;
        this.frequencyHours = frequencyHours;
        this.payloadPercent = payloadPercent;
        this.sequenceFinder = sequenceFinder;
        this.channelUtils = channelUtils;
    }

    @Override
    public VerifierResult call() throws Exception {
        long sequence = getStartSequence();
        int sourceLatest = getSourceLatest();
        String sourceUrl = getSourceUrl();
        String channelUri = replicationUri + "channel/" + getName() + "/";
        VerifierResult result = new VerifierResult(channelUri);
        logger.info("verifying {} from {} to {}, {} items", channelUri, sequence, sourceLatest, sourceLatest - sequence);
        while (sequence <= sourceLatest) {
            Optional<Content> replicatedContent = channelUtils.getContent(channelUri, sequence);
            result.incrementSequencesChecked();
            if (!replicatedContent.isPresent()) {
                result.addMissingSequence(String.valueOf(sequence));
            } else  {
                if (shouldVerifyPayload()) {
                    Optional<Content> sourceContent = channelUtils.getContent(sourceUrl, sequence);
                    if (sourceContent.isPresent()) {
                        result.incrementPayloadsChecked();
                        if (!Arrays.equals(replicatedContent.get().getData(), sourceContent.get().getData())) {
                            result.addMissingSequence(String.valueOf(sequence));
                        }
                    }
                }
            }
            sequence+=1;
        }
        logger.info("completed " + channelUri + " " + result);
        return result;
    }

    private boolean shouldVerifyPayload() {
        return random.nextInt(100) < payloadPercent;
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
