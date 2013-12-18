package com.flightstats.datahub.cluster;

import com.flightstats.datahub.dao.SequenceRowKeyStrategy;
import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.metrics.TimedCallback;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.util.DataHubKeyGenerator;
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
    public DataHubKey newKey(final String channelName) {

        try {
            return metricsTimer.time("hazelcast.newKey", new TimedCallback<DataHubKey>() {
                @Override
                public DataHubKey call() {
                    return nextDataHubKey(channelName);
                }
            });
        } catch (Exception e) {
            throw new RuntimeException("Error generating new DataHubKey: " + channelName, e);
        }
    }

    private DataHubKey nextDataHubKey(String channelName) {
        long sequence = getAtomicNumber(channelName).getAndAdd(1);
        if (isValidSequence(sequence)) {
            return new DataHubKey(sequence);
        }
        return handleMissingKey(channelName);
    }

    private DataHubKey handleMissingKey(final String channelName) {
        logger.warn("sequence number for channel " + channelName + " is not found.  this will over write any existing data!");
        try {
            return channelLockExecutor.execute(channelName, new Callable<DataHubKey>() {
                @Override
                public DataHubKey call() throws Exception {
                    IAtomicLong sequenceNumber = getAtomicNumber(channelName);
                    if (isValidSequence(sequenceNumber.get())) {
                        return nextDataHubKey(channelName);
                    }
                    long currentSequence = SequenceRowKeyStrategy.INCREMENT;
                    sequenceNumber.set(currentSequence + 1);
                    return new DataHubKey(currentSequence);
                }

            });
        } catch (Exception e) {
            throw new RuntimeException("Error generating new DataHubKey: " + channelName, e);
        }
    }

    private boolean isValidSequence(long sequence) {
        return sequence >= SequenceRowKeyStrategy.INCREMENT;
    }

    public void seedChannel(String channelName) {
        getAtomicNumber(channelName).compareAndSet(0, SequenceRowKeyStrategy.INCREMENT);
    }

    private IAtomicLong getAtomicNumber(String channelName) {
        return hazelcastInstance.getAtomicLong("CHANNEL_NAME_SEQ:" + channelName);
    }
}
