package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.util.HubUtils;
import com.google.inject.Inject;

import javax.websocket.WebSocketContainer;

/**
 * Only exists so it can be mocked out.
 */
public class SequenceIteratorFactory {

    private final HubUtils hubUtils;
    private final WebSocketContainer container;

    @Inject
    public SequenceIteratorFactory(HubUtils hubUtils, WebSocketContainer container) {
        this.hubUtils = hubUtils;
        this.container = container;
    }

    public SequenceIterator create(long startSequence, ChannelConfiguration channel) {
        return new SequenceIterator(startSequence, hubUtils, channel, container);
    }
}
