package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class CachedChannelsCollectionDao implements ChannelsCollectionDao {

    public static final String DELEGATE = "CachedChannelsCollectionDao.DELEGATE";
    private final ChannelsCollectionDao delegate;
    private final ConcurrentMap<String,ChannelConfiguration> channelConfigurationMap;

    @Inject
    public CachedChannelsCollectionDao(@Named(DELEGATE) ChannelsCollectionDao delegate,
                                       @Named("ChannelConfigurationMap") ConcurrentMap<String, ChannelConfiguration> channelConfigurationMap) {
        this.delegate = delegate;
        this.channelConfigurationMap = channelConfigurationMap;
    }

    @Override
    public ChannelConfiguration createChannel(String name, Long ttlMillis) {
        ChannelConfiguration channelConfig = delegate.createChannel(name, ttlMillis);
        channelConfigurationMap.put(channelConfig.getName(), channelConfig);
        return channelConfig;
    }

    @Override
    public void updateChannel(ChannelConfiguration newConfig) {
        delegate.updateChannel(newConfig);
        channelConfigurationMap.put(newConfig.getName(), newConfig);
    }

    @Override
    public void initializeMetadata() {
        delegate.initializeMetadata();
    }

    @Override
    public boolean channelExists(String channelName) {
        if (channelConfigurationMap.get(channelName) != null) {
            return true;
        }
        return getChannelConfiguration(channelName) != null;
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        ChannelConfiguration configuration = channelConfigurationMap.get(channelName);
        if (configuration != null) {
            return configuration;
        }
        configuration = delegate.getChannelConfiguration(channelName);
        if (null != configuration) {
            channelConfigurationMap.put(channelName, configuration);
        }
        return configuration;
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return delegate.getChannels();
    }

    @Override
    public boolean isHealthy() {
        return delegate.isHealthy();
    }
}
