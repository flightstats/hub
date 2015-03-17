package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;

public class NasChannelConfigurationDao implements ChannelConfigurationDao {
    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        return null;
    }

    @Override
    public void updateChannel(ChannelConfiguration newConfig) {

    }

    @Override
    public void initialize() {

    }

    @Override
    public boolean channelExists(String channelName) {
        return false;
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        return null;
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return null;
    }

    @Override
    public void delete(String channelName) {

    }
}
