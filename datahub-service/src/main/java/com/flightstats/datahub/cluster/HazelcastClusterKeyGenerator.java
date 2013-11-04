package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.util.DataHubKeyGenerator;
import com.google.inject.Inject;
import com.hazelcast.core.AtomicNumber;
import com.hazelcast.core.HazelcastInstance;

import java.util.concurrent.Callable;

public class HazelcastClusterKeyGenerator implements DataHubKeyGenerator {

    private final HazelcastInstance hazelcastInstance;
    private final ChannelLockExecutor channelLockExecutor;

    @Inject
    public HazelcastClusterKeyGenerator(HazelcastInstance hazelcastInstance, ChannelLockExecutor channelLockExecutor) {
        this.hazelcastInstance = hazelcastInstance;
        this.channelLockExecutor = channelLockExecutor;
    }

    @Override
    public DataHubKey newKey(final String channelName) {
        try {
            //todo - gfm - 11/4/13 - do we still need to get a channel lock for updates?
            Callable<DataHubKey> keyGeneratingCallable = new KeyGeneratingCallable(channelName);
            return channelLockExecutor.execute(channelName, keyGeneratingCallable);
        } catch (Exception e) {
            throw new RuntimeException("Error generating new DataHubKey: ", e);
        }
    }

    private class KeyGeneratingCallable implements Callable<DataHubKey> {
        private final String channelName;

        KeyGeneratingCallable(String channelName) {
            this.channelName = channelName;
        }

        @Override
        public DataHubKey call() throws Exception {
            return new DataHubKey(determineKeySequence());
        }

        private long determineKeySequence() {
            AtomicNumber sequenceNumber = hazelcastInstance.getAtomicNumber("CHANNEL_NAME_SEQ:" + channelName);
            return sequenceNumber.getAndAdd(1);
        }
    }
}
