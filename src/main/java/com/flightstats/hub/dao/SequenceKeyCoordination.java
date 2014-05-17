package com.flightstats.hub.dao;

import com.codahale.metrics.annotation.Timed;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
import com.flightstats.hub.websocket.WebsocketPublisher;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.primitives.Longs;
import com.google.inject.Inject;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.shared.SharedValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public class SequenceKeyCoordination implements KeyCoordination {
    private final static Logger logger = LoggerFactory.getLogger(SequenceKeyCoordination.class);

    private final WebsocketPublisher websocketPublisher;
    private final CuratorFramework curator;
    private final LoadingCache<String, SharedValue> cache;

    @Inject
    public SequenceKeyCoordination(WebsocketPublisher websocketPublisher,
                                   final CuratorFramework curator) {
        this.websocketPublisher = websocketPublisher;
        this.curator = curator;
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
        websocketPublisher.publish(channelName, key);
    }

    @Timed(name = "sequence.setLastUpdated")
    private void setLastUpdateKey(final String channelName, final ContentKey key) {
        final SequenceContentKey sequence = (SequenceContentKey) key;
        try {
            byte[] bytes = Longs.toByteArray(sequence.getSequence());
            SharedValue sharedValue = getSharedValue(channelName);
            int attempts = 0;
            while (attempts < 3) {
                long existing = Longs.fromByteArray(sharedValue.getValue());
                if (sequence.getSequence() > existing) {
                    if (sharedValue.trySetValue(bytes)) return;
                } else {
                    return;
                }
                attempts++;
            }
        } catch (Exception e) {
            logger.warn("unable to set " + channelName + " lastUpdated to " + key, e);
        }
    }

    @VisibleForTesting
    SharedValue getSharedValue(String channelName) {
        return cache.getUnchecked(getKey(channelName));
    }

    @Override
    @Timed(name = "sequence.getLastUpdated")
    public ContentKey getLastUpdated(final String channelName) {
        byte[] value = getSharedValue(channelName).getValue();
        return new SequenceContentKey(Longs.fromByteArray(value));
    }

    @Override
    public void delete(String channelName) {
        String key = getKey(channelName);
        try {
            cache.invalidate(key);
            curator.delete().deletingChildrenIfNeeded().forPath(key);
        } catch (Exception e) {
            logger.warn("unable to delete key " + channelName);
        }
    }

    private String getKey(String channelName) {
        return "/lastUpdated/" + channelName;
    }

}
