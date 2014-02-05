package com.flightstats.hub.dao;

import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.metrics.TimedCallback;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
import com.flightstats.hub.service.ChannelInsertionPublisher;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedValue;

/**
 *
 */
public class SequenceKeyCoordination implements KeyCoordination {
    private final ChannelInsertionPublisher channelInsertionPublisher;
    private final MetricsTimer metricsTimer;
    private final LoadingCache<String, SharedValue> cache;

    @Inject
    public SequenceKeyCoordination(ChannelInsertionPublisher channelInsertionPublisher,
                                   final CuratorFramework curator,
                                   MetricsTimer metricsTimer) {
        this.channelInsertionPublisher = channelInsertionPublisher;
        this.metricsTimer = metricsTimer;
        cache = CacheBuilder.newBuilder().build(new CacheLoader<String, SharedValue>() {
            @Override
            public SharedValue load(String key) throws Exception {
                SharedValue sharedValue = new SharedValue(curator, key, Longs.toByteArray(SequenceContentKey.START_VALUE));
                sharedValue.start();
                return sharedValue;
            }
        });
    }

    @Override
    public void insert(String channelName, ContentKey key) {
        setLastUpdateKey(channelName, key);
        channelInsertionPublisher.publish(channelName, key);
    }

    private void setLastUpdateKey(final String channelName, final ContentKey key) {
        //todo - gfm - 2/4/14 - this might not be accurate when multiple items are added at the same time.
        final SequenceContentKey sequence = (SequenceContentKey) key;
        metricsTimer.time("sequence.setLastUpdated", new TimedCallback<Object>() {
            @Override
            public Object call() {
                try {
                    byte[] bytes = Longs.toByteArray(sequence.getSequence());
                    cache.getUnchecked(getKey(channelName)).setValue(bytes);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return null;
            }
        });
    }

    @Override
    public ContentKey getLastUpdated(final String channelName) {
        return metricsTimer.time("sequence.getLastUpdated", new TimedCallback<ContentKey>() {
            @Override
            public ContentKey call() {
                byte[] value = cache.getUnchecked(getKey(channelName)).getValue();
                return new SequenceContentKey(Longs.fromByteArray(value));
            }
        });
    }

    private String getKey(String channelName) {
        return "/lastUpdated/" + channelName;
    }

}
