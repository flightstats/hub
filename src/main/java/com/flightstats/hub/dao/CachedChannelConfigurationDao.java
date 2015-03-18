package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfiguration;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;

public class CachedChannelConfigurationDao implements ChannelConfigurationDao {

    private final static Logger logger = LoggerFactory.getLogger(CachedChannelConfigurationDao.class);

    public static final String DELEGATE = "CachedChannelMetadataDao.DELEGATE";
    private final ChannelConfigurationDao delegate;
    private final ConcurrentMap<String, ChannelConfiguration> channelConfigurationMap;

    @Inject
    public CachedChannelConfigurationDao(@Named(DELEGATE) ChannelConfigurationDao delegate,
                                         @Named("ChannelConfigurationMap") ConcurrentMap<String, ChannelConfiguration> channelConfigurationMap) {
        this.delegate = delegate;
        this.channelConfigurationMap = channelConfigurationMap;
        logger.info("clearing channelConfigurationMap");
        channelConfigurationMap.clear();
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration config) {
        ChannelConfiguration channelConfig = delegate.createChannel(config);
        channelConfigurationMap.put(channelConfig.getName(), channelConfig);
        return channelConfig;
    }

    @Override
    public void updateChannel(ChannelConfiguration newConfig) {
        delegate.updateChannel(newConfig);
        channelConfigurationMap.put(newConfig.getName(), newConfig);
    }

    @Override
    public void initialize() {
        delegate.initialize();
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
        /**
         * It is very important to use caching here, as getChannelConfiguration is called for every read and write.
         */
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
        /**
         * Using the Hazelcast backed Map is useful when we can directly query the delegate if requested data is missing.
         * However, there is no way to know if we have all of the channels (priamry fear is split-brain Hazelcast, most
         * likely due to misconfiguration), so the safest way is to query the delegate every time.
         */
        return delegate.getChannels();
    }

    @Override
    public void delete(String channelName) {
        delegate.delete(channelName);
        channelConfigurationMap.remove(channelName);
    }

}
