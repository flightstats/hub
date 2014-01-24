package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.*;
import com.flightstats.datahub.service.CreateChannelValidator;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class ChannelServiceImpl implements ChannelService {

    private final static Logger logger = LoggerFactory.getLogger(ChannelServiceImpl.class);

    private final ContentServiceFinder contentServiceFinder;
    private final ChannelMetadataDao channelMetadataDao;
    private final CreateChannelValidator createChannelValidator;
    private final ContentService missingDao = new ContentService() {
        @Override
        public void createChannel(ChannelConfiguration configuration) { }

        @Override
        public void updateChannel(ChannelConfiguration configuration) { }

        @Override
        public ValueInsertionResult insert(ChannelConfiguration configuration, Content content) {
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
        public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
            return Collections.emptyList();
        }

        @Override
        public void delete(String channelName) { }

    };

    @Inject
    public ChannelServiceImpl(ContentServiceFinder contentServiceFinder,
                              ChannelMetadataDao channelMetadataDao, CreateChannelValidator createChannelValidator) {
        this.contentServiceFinder = contentServiceFinder;
        this.channelMetadataDao = channelMetadataDao;
        this.createChannelValidator = createChannelValidator;
    }

    private ContentService getContentService(String channelName){
        ChannelConfiguration channelConfiguration = channelMetadataDao.getChannelConfiguration(channelName);
        if (null == channelConfiguration) {
            logger.info("did not find config for " + channelName);
            return missingDao;
        }
        return contentServiceFinder.getContentService(channelConfiguration);
    }

    @Override
    public boolean channelExists(String channelName) {
        return channelMetadataDao.channelExists(channelName);
    }

    @Override
    public ChannelConfiguration createChannel(ChannelConfiguration configuration) {
        createChannelValidator.validate(configuration);
        configuration = ChannelConfiguration.builder().withChannelConfiguration(configuration).build();
        //todo - gfm - 1/8/14 - this should happen in a channel specific lock
        contentServiceFinder.getContentService(configuration).createChannel(configuration);
        return channelMetadataDao.createChannel(configuration);
    }

    @Override
    public ValueInsertionResult insert(String channelName, Content content) {
        ChannelConfiguration configuration = channelMetadataDao.getChannelConfiguration(channelName);
        return getContentService(channelName).insert(configuration, content);
    }

    @Override
    public Optional<LinkedContent> getValue(String channelName, String id) {
        return getContentService(channelName).getValue(channelName, id);
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
        return getContentService(channelName).findLastUpdatedKey(channelName);
    }

    @Override
    public boolean isHealthy() {
        return channelMetadataDao.isHealthy();
    }

    @Override
    public ChannelConfiguration updateChannel(ChannelConfiguration configuration) {
        configuration = ChannelConfiguration.builder().withChannelConfiguration(configuration).build();
        contentServiceFinder.getContentService(configuration).updateChannel(configuration);
        channelMetadataDao.updateChannel(configuration);
        return configuration;
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        return getContentService(channelName).getKeys(channelName, dateTime);
    }

    @Override
    public void delete(String channelName) {
        getContentService(channelName).delete(channelName);
        channelMetadataDao.delete(channelName);

    }
}
