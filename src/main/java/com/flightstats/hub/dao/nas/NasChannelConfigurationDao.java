package com.flightstats.hub.dao.nas;

import com.flightstats.hub.dao.ChannelConfigDao;
import com.flightstats.hub.model.ChannelConfig;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
        try {
            byte[] bytes = config.toJson().getBytes();
            FileUtils.writeByteArrayToFile(getFile(config.getName()), bytes);
        } catch (IOException e) {
            logger.warn("unable to save config for " + config.getName(), e);
        }
    }

    private File getFile(String name) {
        return new File(channelPath + name);
    }

    @Override
    public void initialize() {
        //todo - gfm - 3/18/15 - anything to do here?
    }

    @Override
    public boolean channelExists(String channelName) {
        return getChannelConfig(channelName) != null;
    }

    @Override
    public ChannelConfig getChannelConfig(String channelName) {
        return readConfig(getFile(channelName));
    }

    private ChannelConfig readConfig(File file) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            return ChannelConfig.fromJson(new String(bytes));
        } catch (IOException e) {
            logger.warn("unable to find config for " + file.getName(), e);
        }
        return null;
    }

    @Override
    public Iterable<ChannelConfig> getChannels() {
        File[] channelFiles = new File(channelPath).listFiles();
        List<ChannelConfig> configs = new ArrayList<>();
        for (int i = 0; i < channelFiles.length; i++) {
            configs.add(readConfig(channelFiles[i]));
        }
        return configs;
    }

    @Override
    public void delete(String channelName) {
        FileUtils.deleteQuietly(getFile(channelName));
    }
}
