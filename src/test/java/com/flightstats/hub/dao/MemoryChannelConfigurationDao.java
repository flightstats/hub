package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class MemoryChannelConfigurationDao implements ChannelConfigurationDao {

    private Map<String, ChannelConfiguration> channelConfiguration = new ConcurrentHashMap<>();

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        channelConfiguration.put(configuration.getName(), configuration);
        return configuration;
    }

    @Override
    public void updateChannel(ChannelConfiguration newConfig) {
        channelConfiguration.put(newConfig.getName(), newConfig);
    }

    @Override
    public void initialize() { }

    @Override
    public boolean channelExists(String channelName) {
        return channelConfiguration.containsKey(channelName);
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        return channelConfiguration.get(channelName);
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return channelConfiguration.values();
    }

    @Override
    public void delete(String channelName) {
        channelConfiguration.remove(channelName);
    }
}
