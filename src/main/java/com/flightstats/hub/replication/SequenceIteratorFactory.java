package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfiguration;
import com.google.inject.Inject;

import javax.websocket.WebSocketContainer;

/**
 * Only exists so it can be mocked out.
 */
public class SequenceIteratorFactory {

    private final ChannelUtils channelUtils;
    private final WebSocketContainer container;

    @Inject
    public SequenceIteratorFactory(ChannelUtils channelUtils, WebSocketContainer container) {
        this.channelUtils = channelUtils;
        this.container = container;
    }

    public SequenceIterator create(long startSequence, ChannelConfiguration channel) {
        return new SequenceIterator(startSequence, channelUtils, channel, container);
    }
}
