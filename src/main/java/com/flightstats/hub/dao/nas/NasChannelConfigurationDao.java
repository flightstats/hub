package com.flightstats.hub.dao.nas;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

public class NasChannelConfigurationDao implements Dao<ChannelConfig> {

    private final static Logger logger = LoggerFactory.getLogger(NasChannelConfigurationDao.class);

    private final String channelPath;

    public NasChannelConfigurationDao() {
        channelPath = NasUtil.getStoragePath() + "channel/";
        logger.info("using channel path {}", channelPath);
    }

    @Override
    public void upsert(ChannelConfig config) {
        NasUtil.writeJson(config.toJson(), config.getName(), channelPath);
    }

    @Override
    public ChannelConfig getCached(String name) {
        return get(name);
    }

    @Override
    public ChannelConfig get(String name) {
        return NasUtil.readJson(channelPath, name, ChannelConfig::fromJson);
    }

    @Override
    public Collection<ChannelConfig> getAll(boolean useCache) {
        return NasUtil.getIterable(channelPath, ChannelConfig::fromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(channelPath + name));
    }
}
