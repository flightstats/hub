package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.exception.MissingKeyException;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.google.inject.Inject;
import com.hazelcast.core.AtomicNumber;
import com.hazelcast.core.HazelcastInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HazelcastClusterKeyGenerator implements DataHubKeyGenerator {

    private final static Logger logger = LoggerFactory.getLogger(HazelcastClusterKeyGenerator.class);

    private final HazelcastInstance hazelcastInstance;

    @Inject
    public HazelcastClusterKeyGenerator(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public DataHubKey newKey(String channelName) {
        try {
            //todo - gfm - 11/10/13 - this needs to make sure the value exists, otherwise find the next value
            return new DataHubKey(getAtomicNumber(channelName).getAndAdd(1));
        } catch (MissingKeyException e) {
            logger.info("sequence number for channel " + channelName + " is not found.  searching");

            //todo - gfm - 11/11/13 - look for existing keys

            return new DataHubKey(getAtomicNumber(channelName).getAndAdd(1));
        }
    }

    public void seedChannel(String channelName) {
        getAtomicNumber(channelName).compareAndSet(0, DataHubKey.MIN_SEQUENCE);
    }

    private AtomicNumber getAtomicNumber(String channelName) {
        return hazelcastInstance.getAtomicNumber("CHANNEL_NAME_SEQ:" + channelName);
    }
}
