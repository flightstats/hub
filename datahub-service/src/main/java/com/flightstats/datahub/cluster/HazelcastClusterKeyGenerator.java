package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.google.inject.Inject;
import com.hazelcast.core.AtomicNumber;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastClusterKeyGenerator implements DataHubKeyGenerator {

    private final HazelcastInstance hazelcastInstance;

    @Inject
    public HazelcastClusterKeyGenerator(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    @Override
    public DataHubKey newKey(final String channelName) {
        try {
            AtomicNumber sequenceNumber = hazelcastInstance.getAtomicNumber("CHANNEL_NAME_SEQ:" + channelName);
            return new DataHubKey(sequenceNumber.getAndAdd(1));
        } catch (Exception e) {
            throw new RuntimeException("Error generating new DataHubKey: ", e);
        }
    }

}
