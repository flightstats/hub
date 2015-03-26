package com.flightstats.hub.dao.nas;

import com.flightstats.hub.dao.ChannelConfigDao;
import com.flightstats.hub.model.ChannelConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class NasChannelConfigurationDao implements ChannelConfigDao {

    private final static Logger logger = LoggerFactory.getLogger(NasChannelConfigurationDao.class);

    private final String channelPath;

    public NasChannelConfigurationDao() {
        channelPath = NasUtil.getStoragePath() + "channel/";
        logger.info("using channel path {}", channelPath);
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig config) {
        updateChannel(config);
        return config;
    }

    @Override
    public void updateChannel(ChannelConfig config) {
        NasUtil.writeJson(config.toJson(), config.getName(), channelPath);
    }

    @Override
    public boolean channelExists(String channelName) {
        return getChannelConfig(channelName) != null;
    }

    @Override
    public ChannelConfig getChannelConfig(String name) {
        return NasUtil.readJson(channelPath, name, ChannelConfig::fromJson);
    }

    @Override
    public Iterable<ChannelConfig> getChannels() {
        return NasUtil.getIterable(channelPath, ChannelConfig::fromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(channelPath + name));
    }
}
