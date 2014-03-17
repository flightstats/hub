package com.flightstats.hub.dao;

import com.flightstats.hub.dao.timeIndex.TimeIndexProcessor;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

public class ContentServiceImpl implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(ContentServiceImpl.class);

    private final ContentDao contentDao;
    private final KeyCoordination keyCoordination;
    private final TimeIndexProcessor timeIndexProcessor;

    @Inject
    public ContentServiceImpl(ContentDao contentDao, KeyCoordination keyCoordination, TimeIndexProcessor timeIndexProcessor) {
        this.contentDao = contentDao;
        this.keyCoordination = keyCoordination;
        this.timeIndexProcessor = timeIndexProcessor;
    }

    @Override
    public void createChannel(ChannelConfiguration configuration) {
        logger.info("Creating channel " + configuration);
        contentDao.initializeChannel(configuration);
    }

    @Override
    public void updateChannel(ChannelConfiguration configuration) {
        logger.info("Updating channel " + configuration);
        contentDao.updateChannel(configuration);
    }

    @Override
    public InsertedContentKey insert(ChannelConfiguration configuration, Content content) {
        String channelName = configuration.getName();
        logger.debug("inserting {} bytes into channel {} ", content.getData().length, channelName);

        InsertedContentKey result = contentDao.write(channelName, content, configuration.getTtlDays());
        keyCoordination.insert(channelName, result.getKey());
        return result;
    }

    @Override
    public Optional<LinkedContent> getValue(String channelName, String id) {
        Optional<ContentKey> keyOptional = contentDao.getKey(id);
        if (!keyOptional.isPresent()) {
            return Optional.absent();
        }
        ContentKey key = keyOptional.get();
        logger.debug("fetching {} from channel {} ", key.toString(), channelName);
        Content value = contentDao.read(channelName, key);
        if (value == null) {
            return Optional.absent();
        }
        ContentKey previous = key.getPrevious();
        ContentKey next = key.getNext();
        if (next != null) {
            Optional<ContentKey> lastUpdatedKey = findLastUpdatedKey(channelName);
            if (lastUpdatedKey.isPresent()) {
                if (lastUpdatedKey.get().equals(key)) {
                    next = null;
                }
            }
        }

        return Optional.of(new LinkedContent(value, previous, next));
    }

    @Override
    public Optional<ContentKey> findLastUpdatedKey(String channelName) {
        return Optional.fromNullable(keyCoordination.getLastUpdated(channelName));
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        return contentDao.getKeys(channelName, dateTime);
    }

    @Override
    public void delete(String channelName) {
        logger.info("deleting channel " + channelName);
        //todo - gfm - 3/17/14 - what else does this need to do?
        contentDao.delete(channelName);
        keyCoordination.delete(channelName);
        timeIndexProcessor.delete(channelName);
    }


}
