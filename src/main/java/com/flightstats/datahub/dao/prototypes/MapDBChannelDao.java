package com.flightstats.datahub.dao.prototypes;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import com.google.common.collect.Maps;
import org.mapdb.DBMaker;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

//sample DAO using MapDB as the backing store.
public class MapDBChannelDao implements ChannelDao {

    private final Map<String, ChannelConfiguration> channelConfigurations = DBMaker.newTempHashMap();
    private final Map<String, Lock> writeLocks = Maps.newConcurrentMap();
    private final Map<String, DataHubKey> latestPerChannel = DBMaker.newTempHashMap();
    private final Map<DataHubKey, LinkedDataHubCompositeValue> channelValues = DBMaker.newTempHashMap();

    @Override
    public boolean channelExists(String channelName) {
        return channelConfigurations.containsKey(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(String name) {
        Date creationDate = new Date();
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
            DataHubKey newKey = new DataHubKey(new Date(), newSequence);

            DataHubCompositeValue dataHubCompositeValue = new DataHubCompositeValue(contentType, data);
            LinkedDataHubCompositeValue newLinkedValue = new LinkedDataHubCompositeValue(dataHubCompositeValue, Optional.fromNullable(oldLastKey), Optional.<DataHubKey>absent());
            //first put the actual value in.
            channelValues.put(newKey, newLinkedValue);
            //then link the old previous to the new value
            if (oldLastKey != null) {
                LinkedDataHubCompositeValue previousLinkedValue = channelValues.get(oldLastKey);
                channelValues.put(oldLastKey, new LinkedDataHubCompositeValue(previousLinkedValue.getValue(), previousLinkedValue.getPrevious(), Optional.of(newKey)));
            }
            //finally, make it the latest
            latestPerChannel.put(channelName, newKey);
            return new ValueInsertionResult(newKey);
        }
        finally {
            lock.unlock();
        }
    }

    @Override
    public Optional<LinkedDataHubCompositeValue> getValue(String channelName, DataHubKey key) {
        return Optional.of(channelValues.get(key));
    }

    @Override
    public Optional<DataHubKey> findLatestId(String channelName) {
        DataHubKey key = latestPerChannel.get(channelName);
        return Optional.fromNullable(key);
    }
}
