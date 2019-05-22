package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;

@Slf4j
public class FileChannelConfigurationDao implements Dao<ChannelConfig> {

    private final FileUtil fileUtil;
    private final String channelPath;

    @Inject
    public FileChannelConfigurationDao(FileUtil fileUtil) {
        this.fileUtil = fileUtil;
        this.channelPath = fileUtil.getStoragePath() + "channel/";
        log.info("using channel path {}", channelPath);
    }

    @Override
    public void upsert(ChannelConfig config) {
        fileUtil.write(config.toJson(), config.getLowerCaseName(), channelPath);
    }

    @Override
    public ChannelConfig get(String name) {
        return fileUtil.read(channelPath, name.toLowerCase(), ChannelConfig::createFromJson);
    }

    @Override
    public Collection<ChannelConfig> getAll(boolean useCache) {
        return fileUtil.getIterable(channelPath, ChannelConfig::createFromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(channelPath + name.toLowerCase()));
    }
}
