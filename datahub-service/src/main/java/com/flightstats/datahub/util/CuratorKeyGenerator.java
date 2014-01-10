package com.flightstats.datahub.util;

import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.metrics.TimedCallback;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.atomic.AtomicValue;
import org.apache.curator.framework.recipes.atomic.DistributedAtomicLong;
import org.apache.curator.framework.recipes.atomic.PromotedToLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * todo - gfm - 12/19/13 - test what happens with no quorum
 */
public class CuratorKeyGenerator implements DataHubKeyGenerator {
    private final static Logger logger = LoggerFactory.getLogger(CuratorKeyGenerator.class);

    private final CuratorFramework client;
    private final MetricsTimer metricsTimer;
    private final RetryPolicy retryPolicy;
    private ConcurrentMap<String, DistributedAtomicLong> channelToLongMap = new ConcurrentHashMap<>();

    @Inject
    public CuratorKeyGenerator(CuratorFramework client, MetricsTimer metricsTimer, RetryPolicy retryPolicy) {
        this.client = client;
        this.metricsTimer = metricsTimer;
        this.retryPolicy = retryPolicy;
    }

    @Override
    public SequenceContentKey newKey(final String channelName) {
        return metricsTimer.time("keyGen.newKey", new TimedCallback<SequenceContentKey>() {
            @Override
            public SequenceContentKey call() {
                return getDataHubKey(channelName);
            }
        });
    }

    private SequenceContentKey getDataHubKey(String channelName) {
        DistributedAtomicLong atomicLong = getDistributedAtomicLong(channelName);
        try {
            AtomicValue<Long> value = atomicLong.increment();
            if (value.succeeded()) {
                return new SequenceContentKey(value.postValue());
            } else {
                //todo - gfm - 12/17/13 - do what?
                logger.warn("not sure what this means " + channelName + " " + value);
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
            atomicLong.trySet(999L);
            logger.info("seeded channel " + channelName);
        } catch (Exception e) {
            logger.warn("unable to seed " + channelName, e);
        }
    }

    @Override
    public Optional<ContentKey> parse(String keyString) {
        return SequenceContentKey.fromString(keyString);
    }

    private DistributedAtomicLong getDistributedAtomicLong(String channelName) {
        DistributedAtomicLong atomicLong = channelToLongMap.get(channelName);
        if (null == atomicLong) {
            String path = "/keyGenerator/" + channelName;
            PromotedToLock lock = PromotedToLock.builder().lockPath(path + "/lock")
                    .retryPolicy(retryPolicy).timeout(1000, TimeUnit.SECONDS).build();
            atomicLong = new DistributedAtomicLong(client, path, retryPolicy, lock);
            DistributedAtomicLong previousLong = channelToLongMap.putIfAbsent(path, atomicLong);
            if (previousLong != null) {
                atomicLong = previousLong;
            }
        }
        return atomicLong;
    }
}
