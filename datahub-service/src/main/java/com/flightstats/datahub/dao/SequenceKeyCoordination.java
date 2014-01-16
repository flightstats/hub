package com.flightstats.datahub.dao;

import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.metrics.TimedCallback;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.service.ChannelInsertionPublisher;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class SequenceKeyCoordination implements KeyCoordination {

    private final ConcurrentMap<String, ContentKey> lastUpdatedPerChannel;
    private final ChannelInsertionPublisher channelInsertionPublisher;
    private final MetricsTimer metricsTimer;

    @Inject
    public SequenceKeyCoordination(@Named("LastUpdatePerChannelMap") ConcurrentMap<String, ContentKey> lastUpdatedPerChannel,
                                   ChannelInsertionPublisher channelInsertionPublisher,
                                   MetricsTimer metricsTimer) {
        this.lastUpdatedPerChannel = lastUpdatedPerChannel;
        this.channelInsertionPublisher = channelInsertionPublisher;
        this.metricsTimer = metricsTimer;
    }

    @Override
    public void insert(String channelName, ContentKey key) {
        setLastUpdateKey(channelName, key);
        channelInsertionPublisher.publish(channelName, key);
    }

    private void setLastUpdateKey(final String channelName, final ContentKey key) {
        //this is relatively slow with hazelcast and high throughput
        metricsTimer.time("hazelcast.setLastUpdated", new TimedCallback<Object>() {
            @Override
            public Object call() {
                lastUpdatedPerChannel.put(channelName, key);
                return null;
            }
        });
    }

    @Override
    public ContentKey getLastUpdated(final String channelName) {
        return metricsTimer.time("hazelcast.getLastUpdated", new TimedCallback<ContentKey>() {
            @Override
            public ContentKey call() {
                return lastUpdatedPerChannel.get(channelName);
            }
        });
    }
}
