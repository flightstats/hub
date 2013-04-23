package com.flightstats.datahub.dao.prototypes;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.flightstats.datahub.util.TimeProvider;
import com.google.common.base.Optional;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class InMemoryChannelDao implements ChannelDao {
    private final TimeProvider timeProvider;

    private final Map<String, ChannelConfiguration> channelConfigurations = Maps.newConcurrentMap();
    private final Map<String, Lock> writeLocks = Maps.newConcurrentMap();
    private final Map<String, DataHubKey> latestPerChannel = Maps.newConcurrentMap();
    private final Cache<DataHubKey, LinkedDataHubCompositeValue> channelValues = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();

    @Inject
    public InMemoryChannelDao(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelConfigurations.containsKey(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(String name) {
        Date creationDate = timeProvider.getDate();
        ChannelConfiguration channelConfiguration = new ChannelConfiguration(name, creationDate, null);
        writeLocks.put(name, new ReentrantLock());
        channelConfigurations.put(name, channelConfiguration);
        return channelConfiguration;
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        ChannelConfiguration channelConfiguration = channelConfigurations.get(channelName);
        return new ChannelConfiguration(channelConfiguration.getName(), channelConfiguration.getCreationDate(), findLatestId(channelConfiguration.getName()).orNull());
    }

    @Override
    public int countChannels() {
        return channelConfigurations.size();
    }

    @Override
    public ValueInsertionResult insert(String channelName, String contentType, byte[] data) {
        Lock lock = writeLocks.get(channelName);
        lock.lock();
        try {
            DataHubKey oldLastKey = latestPerChannel.get(channelName);
            short newSequence = (oldLastKey == null) ? ((short) 0) : (short) (oldLastKey.getSequence() + 1);
            DataHubKey newKey = new DataHubKey(timeProvider.getDate(), newSequence);

            DataHubCompositeValue dataHubCompositeValue = new DataHubCompositeValue(contentType, data);
            LinkedDataHubCompositeValue newLinkedValue = new LinkedDataHubCompositeValue(dataHubCompositeValue, Optional.fromNullable(oldLastKey), Optional.<DataHubKey>absent());
            //first put the actual value in.
            channelValues.put(newKey, newLinkedValue);
            //then link the old previous to the new value
            if (oldLastKey != null) {
                LinkedDataHubCompositeValue previousLinkedValue = channelValues.getIfPresent(oldLastKey);
                //in case the value has expired.
                if (previousLinkedValue != null) {
                    channelValues.put(oldLastKey, new LinkedDataHubCompositeValue(previousLinkedValue.getValue(), previousLinkedValue.getPrevious(), Optional.of(newKey)));
                }
            }
            //finally, make it the latest
            latestPerChannel.put(channelName, newKey);
            return new ValueInsertionResult(newKey);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
        return Optional.fromNullable(channelValues.getIfPresent(key));
    }

    @Override
    public Optional<DataHubKey> findLatestId(String channelName) {
        DataHubKey key = latestPerChannel.get(channelName);
        return Optional.fromNullable(key);
    }
}
