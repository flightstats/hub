package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.concurrent.ConcurrentMap;

/**
 *
 */
public class CachedChannelMetadataDao implements ChannelMetadataDao {

    public static final String DELEGATE = "CachedChannelMetadataDao.DELEGATE";
    private final ChannelMetadataDao delegate;
    private final ConcurrentMap<String,ChannelConfiguration> channelConfigurationMap;

    @Inject
    public CachedChannelMetadataDao(@Named(DELEGATE) ChannelMetadataDao delegate,
                                    @Named("ChannelConfigurationMap") ConcurrentMap<String, ChannelConfiguration> channelConfigurationMap) {
        this.delegate = delegate;
        this.channelConfigurationMap = channelConfigurationMap;
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        ChannelConfiguration channelConfig = delegate.createChannel(configuration);
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
    public void delete(String channelName) {
        delegate.delete(channelName);
        channelConfigurationMap.remove(channelName);
    }

    @Override
    public boolean isHealthy() {
        return delegate.isHealthy();
    }
}
