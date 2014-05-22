package com.flightstats.hub.replication;

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

    public SequenceIterator create(long startSequence, Channel channel) {
        return new SequenceIterator(startSequence, channelUtils, channel, container);
    }
}
