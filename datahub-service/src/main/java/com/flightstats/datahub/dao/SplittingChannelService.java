package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ContentKey;
import com.flightstats.datahub.model.LinkedContent;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 *
 */
public class SplittingChannelService implements ChannelService {
    //todo - gfm - 12/30/13 - would be nice to merge this with SimpleChannelService

    private final static Logger logger = LoggerFactory.getLogger(SplittingChannelService.class);

    //todo - gfm - 12/30/13 - limit exceptions propagated up the stack
    private final ContentService sequentialDao;
    private final ContentService timeSeriesDao;
    private final ChannelMetadataDao channelMetadataDao;
    private final ContentService missingDao = new ContentService() {
        @Override
        public void createChannel(ChannelConfiguration configuration) {
        }

        @Override
        public ValueInsertionResult insert(ChannelConfiguration configuration, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
            return null;
        }

        @Override
        public Optional<LinkedContent> getValue(String channelName, String id) {
            return Optional.absent();
        }

        @Override
        public Optional<ContentKey> findLastUpdatedKey(String channelName) {
            return Optional.absent();
        }

        @Override
        public Iterable<ContentKey> getKeys(ChannelConfiguration configuration, DateTime dateTime) {
            return Collections.emptyList();
        }

        @Override
        public void delete(String channelName) {
            //do nothing
        }

    };

    @Inject
    public SplittingChannelService(@Sequential ContentService sequentialDao,
                                   @TimeSeries ContentService timeSeriesDao,
                                   ChannelMetadataDao channelMetadataDao) {
        this.sequentialDao = sequentialDao;
        this.timeSeriesDao = timeSeriesDao;
        this.channelMetadataDao = channelMetadataDao;
    }

    private ContentService getChannelDao(String channelName){
        ChannelConfiguration channelConfiguration = channelMetadataDao.getChannelConfiguration(channelName);
        if (null == channelConfiguration) {
            logger.info("did not find config for " + channelName);
            return missingDao;
        }
        return getChannelDao(channelConfiguration);
    }

    private ContentService getChannelDao(ChannelConfiguration channelConfiguration) {
        if (channelConfiguration.isSequence()) {
            return sequentialDao;
        }
        return timeSeriesDao;
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelMetadataDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        getChannelDao(configuration).createChannel(configuration);
        return channelMetadataDao.createChannel(configuration);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Optional<String> contentType, Optional<String> contentLanguage, byte[] data) {
        ChannelConfiguration configuration = channelMetadataDao.getChannelConfiguration(channelName);
        return getChannelDao(channelName).insert(configuration, contentType, contentLanguage, data);
    }

    @Override
    public Optional<LinkedContent> getValue(String channelName, String id) {
        return getChannelDao(channelName).getValue(channelName, id);
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
        return getChannelDao(channelName).findLastUpdatedKey(channelName);
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
        ChannelConfiguration configuration = channelMetadataDao.getChannelConfiguration(channelName);
        return getChannelDao(channelName).getKeys(configuration, dateTime);
    }

    @Override
    public void delete(String channelName) {
        getChannelDao(channelName).delete(channelName);
        channelMetadataDao.delete(channelName);

    }
}
