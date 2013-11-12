package com.flightstats.datahub.cluster;

import com.flightstats.datahub.dao.LastKeyFinder;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.exception.MissingKeyException;
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
            try {
                return nextDataHubKey(channelName);
            } catch (MissingKeyException e) {
                logger.info("sequence number for channel " + channelName + " is not found.  searching");
                return channelLockExecutor.execute(channelName, new Callable<DataHubKey>() {
                    @Override
                    public DataHubKey call() throws Exception {
                        if (DataHubKey.isValidSequence(getAtomicNumber(channelName).get())) {
                            return nextDataHubKey(channelName);
                        }
                        DataHubKey latestKey = lastKeyFinder.queryForLatestKey(channelName);
                        if (null == latestKey) {
                            getAtomicNumber(channelName).set(DataHubKey.MIN_SEQUENCE);
                        } else {
                            getAtomicNumber(channelName).set(latestKey.getSequence() + 1);
                        }

                        return nextDataHubKey(channelName);
                    }
                });
            }
        } catch (Exception e) {
            throw new RuntimeException("Error generating new DataHubKey: ", e);
        }
    }

    private DataHubKey nextDataHubKey(String channelName) {
        return new DataHubKey(getAtomicNumber(channelName).getAndAdd(1));
    }

    public void seedChannel(String channelName) {
        getAtomicNumber(channelName).compareAndSet(0, DataHubKey.MIN_SEQUENCE);
    }

    private AtomicNumber getAtomicNumber(String channelName) {
        return hazelcastInstance.getAtomicNumber("CHANNEL_NAME_SEQ:" + channelName);
    }
}
