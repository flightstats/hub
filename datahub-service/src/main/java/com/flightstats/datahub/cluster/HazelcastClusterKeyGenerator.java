package com.flightstats.datahub.cluster;

import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.metrics.TimedCallback;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.SequenceContentKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;

/**
 * This would be simpler with Zookeeper's persisted values
 */
public class HazelcastClusterKeyGenerator implements DataHubKeyGenerator {

    private final static Logger logger = LoggerFactory.getLogger(HazelcastClusterKeyGenerator.class);
    public static final int STARTING_SEQUENCE = 1000;

    private final HazelcastInstance hazelcastInstance;
    private ChannelLockExecutor channelLockExecutor;
    private final MetricsTimer metricsTimer;

    @Inject
    public HazelcastClusterKeyGenerator(HazelcastInstance hazelcastInstance,
                                        ChannelLockExecutor channelLockExecutor, MetricsTimer metricsTimer) {
        this.hazelcastInstance = hazelcastInstance;
        this.channelLockExecutor = channelLockExecutor;
        this.metricsTimer = metricsTimer;
    }

    @Override
    public SequenceContentKey newKey(final String channelName) {

        try {
            return metricsTimer.time("keyGen.newKey", new TimedCallback<SequenceContentKey>() {
                @Override
                public SequenceContentKey call() {
                    return nextDataHubKey(channelName);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error generating new ContentKey: " + channelName, e);
        }
    }

    private SequenceContentKey nextDataHubKey(String channelName) {
        long sequence = getAtomicNumber(channelName).getAndAdd(1);
        if (isValidSequence(sequence)) {
            return new SequenceContentKey(sequence);
        }
        return handleMissingKey(channelName);
    }

    private SequenceContentKey handleMissingKey(final String channelName) {
        logger.warn("sequence number for channel " + channelName + " is not found.  this will over write any existing data!");
        try {
            return channelLockExecutor.execute(channelName, new Callable<SequenceContentKey>() {
                @Override
                public SequenceContentKey call() throws Exception {
                    IAtomicLong sequenceNumber = getAtomicNumber(channelName);
                    if (isValidSequence(sequenceNumber.get())) {
                        return nextDataHubKey(channelName);
                    }
                    long currentSequence = STARTING_SEQUENCE;
                    sequenceNumber.set(currentSequence + 1);
                    return new SequenceContentKey(currentSequence);
                }

            });
        } catch (Exception e) {
            throw new RuntimeException("Error generating new ContentKey: " + channelName, e);
        }
    }

    private boolean isValidSequence(long sequence) {
        return sequence >= STARTING_SEQUENCE;
    }

    public void seedChannel(String channelName) {
        getAtomicNumber(channelName).compareAndSet(0, STARTING_SEQUENCE);
    }

    @Override
    public Optional<ContentKey> parse(String keyString) {
        return SequenceContentKey.fromString(keyString);
    }

    private IAtomicLong getAtomicNumber(String channelName) {
        return hazelcastInstance.getAtomicLong("CHANNEL_NAME_SEQ:" + channelName);
    }
}
