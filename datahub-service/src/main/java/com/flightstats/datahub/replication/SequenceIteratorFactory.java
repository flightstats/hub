package com.flightstats.datahub.replication;

/**
 * Only exists so it can be mocked out.
 */
public class SequenceIteratorFactory {

    public SequenceIterator create(long startSequence, ChannelUtils channelUtils, String channelUrl) {
        return new SequenceIterator(startSequence, channelUtils, channelUrl);
    }
}
