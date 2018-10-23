package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;

public class FileChannelConfigurationDao implements Dao<ChannelConfig> {

    private final static Logger logger = LoggerFactory.getLogger(FileChannelConfigurationDao.class);

    private final ChannelService channelService;
    private final Gson gson;
    private final String channelPath;

    @Inject
    public FileChannelConfigurationDao(ChannelService channelService, Gson gson) {
        this.channelService = channelService;
        this.gson = gson;
        channelPath = FileUtil.getStoragePath() + "channel/";
        logger.info("using channel path {}", channelPath);
    }

    @Override
    public void upsert(ChannelConfig config) {
        FileUtil.write(gson.toJson(config), config.getLowerCaseName(), channelPath);
    }

    @Override
    public ChannelConfig get(String name) {
        return FileUtil.read(channelPath, name.toLowerCase(), channelService::createFromJson);
    }

    @Override
    public Collection<ChannelConfig> getAll(boolean useCache) {
        return FileUtil.getIterable(channelPath, channelService::createFromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(channelPath + name.toLowerCase()));
    }
}
