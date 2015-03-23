package com.flightstats.hub.dao;

import com.flightstats.hub.model.ChannelConfig;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;

public class CachedChannelConfigDao implements ChannelConfigDao {

    private final static Logger logger = LoggerFactory.getLogger(CachedChannelConfigDao.class);

    public static final String DELEGATE = "CachedChannelMetadataDao.DELEGATE";
    private final ChannelConfigDao delegate;
    private final ConcurrentMap<String, ChannelConfig> channelConfigurationMap;

    @Inject
    public CachedChannelConfigDao(@Named(DELEGATE) ChannelConfigDao delegate,
                                  @Named("ChannelConfigurationMap") ConcurrentMap<String, ChannelConfig> channelConfigurationMap) {
        this.delegate = delegate;
        this.channelConfigurationMap = channelConfigurationMap;
        logger.info("clearing channelConfigurationMap");
        channelConfigurationMap.clear();
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig config) {
        ChannelConfig channelConfig = delegate.createChannel(config);
        channelConfigurationMap.put(channelConfig.getName(), channelConfig);
        return channelConfig;
    }

    @Override
    public void updateChannel(ChannelConfig newConfig) {
        delegate.updateChannel(newConfig);
        channelConfigurationMap.put(newConfig.getName(), newConfig);
    }

    @Override
    public void initialize() {
        delegate.initialize();
    }

    @Override
    public boolean channelExists(String name) {
        if (channelConfigurationMap.get(name) != null) {
            return true;
        }
        return getChannelConfig(name) != null;
    }

    @Override
    public ChannelConfig getChannelConfig(String name) {
        /**
         * It is very important to use caching here, as getChannelConfig is called for every read and write.
         */
        ChannelConfig configuration = channelConfigurationMap.get(name);
        if (configuration != null) {
            return configuration;
        }
        configuration = delegate.getChannelConfig(name);
        if (null != configuration) {
            channelConfigurationMap.put(name, configuration);
        }
        return configuration;
    }

    @Override
    public Iterable<ChannelConfig> getChannels() {
        /**
         * Using the Hazelcast backed Map is useful when we can directly query the delegate if requested data is missing.
         * However, there is no way to know if we have all of the channels (priamry fear is split-brain Hazelcast, most
         * likely due to misconfiguration), so the safest way is to query the delegate every time.
         */
        return delegate.getChannels();
    }

    @Override
    public void delete(String name) {
        delegate.delete(name);
        channelConfigurationMap.remove(name);
    }

}
