package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.LinkedContent;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;

/**
 *
 */
public class SimpleChannelService implements ChannelService {

    private final ContentService contentService;
    private final ChannelMetadataDao channelMetadataDao;

    @Inject
    public SimpleChannelService(ContentService contentService,
                                ChannelMetadataDao channelMetadataDao) {
        this.contentService = contentService;
        this.channelMetadataDao = channelMetadataDao;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelMetadataDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        contentService.createChannel(configuration);
        return channelMetadataDao.createChannel(configuration);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        return contentService.insert(channelMetadataDao.getChannelConfiguration(channelName), contentType, contentLanguage, data);
    }

    @Override
    public Optional<LinkedContent> getValue(String channelName, String id) {
        return contentService.getValue(channelName, id);
    }

    @Override
    public ChannelConfiguration getChannelConfiguration(String channelName) {
        return channelMetadataDao.getChannelConfiguration(channelName);
    }

    @Override
    public Iterable<ChannelConfiguration> getChannels() {
        return channelMetadataDao.getChannels();
    }

    @Override
    public Optional<ContentKey> findLastUpdatedKey(String channelName) {
        return contentService.findLastUpdatedKey(channelName);
    }

    @Override
    public boolean isHealthy() {
        return channelMetadataDao.isHealthy();
    }

    @Override
    public void updateChannelMetadata(ChannelConfiguration newConfig) {
        channelMetadataDao.updateChannel(newConfig);
    }

    @Override
    public Iterable<ContentKey> getKeys(String channelName, DateTime dateTime) {
        return contentService.getKeys(channelName, dateTime);
    }
}
