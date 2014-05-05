package com.flightstats.hub.util;

import com.flightstats.hub.metrics.MetricsTimer;
import com.flightstats.hub.metrics.TimedCallback;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.SequenceContentKey;
import com.google.common.base.Optional;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.Inject;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.atomic.PromotedToLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class CuratorKeyGenerator implements ContentKeyGenerator {
    private final static Logger logger = LoggerFactory.getLogger(CuratorKeyGenerator.class);

    private final CuratorFramework curator;
    private final MetricsTimer metricsTimer;
    private LoadingCache<String, DistributedAtomicLong> atomicLongCache;

    @Inject
    public CuratorKeyGenerator(final CuratorFramework curator, MetricsTimer metricsTimer, final RetryPolicy retryPolicy) {
        this.curator = curator;
        this.metricsTimer = metricsTimer;
        atomicLongCache = CacheBuilder.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .build(new CacheLoader<String, DistributedAtomicLong>() {
                    @Override
                    public DistributedAtomicLong load(String channelName) throws Exception {
                        String path = getPath(channelName);
                        PromotedToLock lock = PromotedToLock.builder().lockPath(path + "/lock")
                                .retryPolicy(retryPolicy).timeout(1000, TimeUnit.SECONDS).build();
                        return new DistributedAtomicLong(curator, path, retryPolicy, lock);
                    }
                });
    }

    @Override
    public SequenceContentKey newKey(final String channelName) {
        return metricsTimer.time("keyGen.newKey", new TimedCallback<SequenceContentKey>() {
            @Override
            public SequenceContentKey call() {
                return getContentKey(channelName);
            }
        });
    }

    private SequenceContentKey getContentKey(String channelName) {
        DistributedAtomicLong atomicLong = getDistributedAtomicLong(channelName);
        try {
            AtomicValue<Long> value = atomicLong.increment();
            if (value.succeeded()) {
                return new SequenceContentKey(value.postValue());
            }
        } catch (Exception e) {
            logger.warn("unable to set atomiclong " + channelName, e);
        }
        throw new RuntimeException("what??? " + channelName);
    }

    @Override
    public void seedChannel(String channelName) {
        DistributedAtomicLong atomicLong = getDistributedAtomicLong(channelName);
        try {
            atomicLong.trySet(SequenceContentKey.START_VALUE);
            logger.info("seeded channel " + channelName);
        } catch (Exception e) {
            logger.warn("unable to seed " + channelName, e);
        }
    }

    @Override
    public Optional<ContentKey> parse(String keyString) {
        return SequenceContentKey.fromString(keyString);
    }

    @Override
    public void delete(String channelName) {
        String path = getPath(channelName);
        try {
            atomicLongCache.invalidate(channelName);
            curator.delete().deletingChildrenIfNeeded().forPath(path);
        } catch (Exception e) {
            logger.warn("unable to delete " + channelName, e);
        }
    }

    @Override
    public void setLatest(String channelName, ContentKey contentKey) {
        DistributedAtomicLong atomicLong = getDistributedAtomicLong(channelName);
        SequenceContentKey sequenceContentKey = (SequenceContentKey) contentKey;
        try {
            atomicLong.forceSet(sequenceContentKey.getSequence());
        } catch (Exception e) {
            logger.warn("unable to set content key sequence", e);
        }
    }

    private DistributedAtomicLong getDistributedAtomicLong(String channelName) {
        return atomicLongCache.getUnchecked(channelName);
    }

    private String getPath(String channelName) {
        return "/keyGenerator/" + channelName;
    }
}
