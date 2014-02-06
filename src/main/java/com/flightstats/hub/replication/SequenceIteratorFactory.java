package com.flightstats.hub.replication;

import com.codahale.metrics.MetricRegistry;
import com.google.inject.Inject;

import javax.websocket.WebSocketContainer;

/**
 * Only exists so it can be mocked out.
 */
public class SequenceIteratorFactory {

    private final ChannelUtils channelUtils;
    private final WebSocketContainer container;
    private final MetricRegistry metricRegistry;

    @Inject
    public SequenceIteratorFactory(ChannelUtils channelUtils, WebSocketContainer container, MetricRegistry metricRegistry) {
        this.channelUtils = channelUtils;
        this.container = container;
        this.metricRegistry = metricRegistry;
    }

    public SequenceIterator create(long startSequence, Channel channel) {
        return new SequenceIterator(startSequence, channelUtils, channel, container, metricRegistry);
    }
}
