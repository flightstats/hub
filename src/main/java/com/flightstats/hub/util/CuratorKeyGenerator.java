package com.flightstats.hub.util;

import com.codahale.metrics.annotation.Timed;
import com.flightstats.hub.model.ContentKey;
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

public class CuratorKeyGenerator implements ContentKeyGenerator {
    private final static Logger logger = LoggerFactory.getLogger(CuratorKeyGenerator.class);

    private final CuratorFramework curator;
    private final RetryPolicy retryPolicy;
    private ConcurrentMap<String, DistributedAtomicLong> channelToLongMap = new ConcurrentHashMap<>();

    @Inject
    public CuratorKeyGenerator(CuratorFramework curator, RetryPolicy retryPolicy) {
        this.curator = curator;
        this.retryPolicy = retryPolicy;
    }

    @Override
    @Timed(name = "keyGen.newKey")
    public ContentKey newKey(final String channelName) {
        return getContentKey(channelName);
    }

    private ContentKey getContentKey(String channelName) {
        DistributedAtomicLong atomicLong = getDistributedAtomicLong(channelName);
        try {
            AtomicValue<Long> value = atomicLong.increment();
            if (value.succeeded()) {
                return new ContentKey(value.postValue());
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
            atomicLong.trySet(ContentKey.START_VALUE);
            logger.info("seeded channel " + channelName);
        } catch (Exception e) {
            logger.warn("unable to seed " + channelName, e);
        }
    }

    @Override
    public Optional<ContentKey> parse(String keyString) {
        return ContentKey.fromString(keyString);
    }

    @Override
    public void delete(String channelName) {
        String path = getPath(channelName);
        try {
            curator.delete().deletingChildrenIfNeeded().forPath(path);
            channelToLongMap.remove(channelName);
        } catch (Exception e) {
            logger.warn("unable to delete " + channelName, e);
        }

    }

    @Override
    public void setLatest(String channelName, ContentKey contentKey) {
        DistributedAtomicLong atomicLong = getDistributedAtomicLong(channelName);
        try {
            atomicLong.forceSet(contentKey.getSequence());
        } catch (Exception e) {
            logger.warn("unable to set content key sequence", e);
        }
    }

    private DistributedAtomicLong getDistributedAtomicLong(String channelName) {
        DistributedAtomicLong atomicLong = channelToLongMap.get(channelName);
        if (null == atomicLong) {
            String path = getPath(channelName);
            PromotedToLock lock = PromotedToLock.builder().lockPath(path + "/lock")
                    .retryPolicy(retryPolicy).timeout(1000, TimeUnit.SECONDS).build();
            atomicLong = new DistributedAtomicLong(curator, path, retryPolicy, lock);
            DistributedAtomicLong previousLong = channelToLongMap.putIfAbsent(channelName, atomicLong);
            if (previousLong != null) {
                atomicLong = previousLong;
            }
        }
        return atomicLong;
    }

    private String getPath(String channelName) {
        return "/keyGenerator/" + channelName;
    }
}
