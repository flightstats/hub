package com.flightstats.datahub.cluster;

import com.flightstats.datahub.dao.LastKeyFinder;
import com.flightstats.datahub.dao.SequenceRowKeyStrategy;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.google.inject.Inject;
import com.hazelcast.core.AtomicNumber;
import com.hazelcast.core.HazelcastInstance;
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
    private LastKeyFinder lastKeyFinder;

    @Inject
    public HazelcastClusterKeyGenerator(HazelcastInstance hazelcastInstance,
                                        ChannelLockExecutor channelLockExecutor,
                                        LastKeyFinder lastKeyFinder) {
        this.hazelcastInstance = hazelcastInstance;
        this.channelLockExecutor = channelLockExecutor;
        this.lastKeyFinder = lastKeyFinder;
    }

    @Override
    public DataHubKey newKey(final String channelName) {
        try {
            return nextDataHubKey(channelName);
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
        logger.info("sequence number for channel " + channelName + " is not found.  searching");
        try {
            return channelLockExecutor.execute(channelName, new Callable<DataHubKey>() {
                @Override
                public DataHubKey call() throws Exception {
                    AtomicNumber sequenceNumber = getAtomicNumber(channelName);
                    if (isValidSequence(sequenceNumber.get())) {
                        return nextDataHubKey(channelName);
                    }
                    long currentSequence = findCurrentSequence();
                    sequenceNumber.set(currentSequence + 1);
                    return new DataHubKey(currentSequence);
                }

                private long findCurrentSequence() {
                    DataHubKey latestKey = lastKeyFinder.queryForLatestKey(channelName);
                    if (null == latestKey) {
                        return SequenceRowKeyStrategy.INCREMENT;
                    }
                    return latestKey.getSequence() + 1;
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

    private AtomicNumber getAtomicNumber(String channelName) {
        return hazelcastInstance.getAtomicNumber("CHANNEL_NAME_SEQ:" + channelName);
    }
}
