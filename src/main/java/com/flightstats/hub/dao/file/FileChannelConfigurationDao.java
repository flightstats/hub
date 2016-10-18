package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

public class FileChannelConfigurationDao implements Dao<ChannelConfig> {

    private final static Logger logger = LoggerFactory.getLogger(FileChannelConfigurationDao.class);

    private final String channelPath;

    public FileChannelConfigurationDao() {
        channelPath = FileUtil.getStoragePath() + "channel/";
        logger.info("using channel path {}", channelPath);
    }

    @Override
    public void upsert(ChannelConfig config) {
        FileUtil.writeJson(config.toJson(), config.getName(), channelPath);
    }

    @Override
    public ChannelConfig get(String name) {
        return FileUtil.readJson(channelPath, name, ChannelConfig::createFromJson);
    }

    @Override
    public Collection<ChannelConfig> getAll(boolean useCache) {
        return FileUtil.getIterable(channelPath, ChannelConfig::createFromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(channelPath + name));
    }
}
