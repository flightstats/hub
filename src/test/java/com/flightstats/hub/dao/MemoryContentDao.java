package com.flightstats.hub.dao;

import com.flightstats.hub.dao.timeIndex.TimeIndexDao;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class MemoryContentDao implements ContentDao, TimeIndexDao {
    private final static Logger logger = LoggerFactory.getLogger(MemoryContentDao.class);

    private ListMultimap<String, Content> contentListMultimap = ArrayListMultimap.create();
    private Map<String, SequenceContentKey> contentKeyMap = new HashMap<>();

    @Override
    public synchronized InsertedContentKey write(String channelName, Content content, long ttlDays) {

        List<Content> contentList = contentListMultimap.get(channelName);
        SequenceContentKey existing = contentKeyMap.get(channelName);
        SequenceContentKey key = new SequenceContentKey(existing.getSequence() + 1);
        logger.info("writing " + key.keyToString());
        content.setContentKey(key);
        contentList.add(content);
        contentKeyMap.put(channelName, key);
        return new InsertedContentKey(key, new Date(content.getMillis()));
    }

    @Override
    public synchronized Content read(String channelName, ContentKey key) {
        logger.info("reading " + key.keyToString());
        int sequence = Integer.parseInt(key.keyToString());
        List<Content> contentList = contentListMultimap.get(channelName);
        try {
            return contentList.get(sequence - 1000);
        } catch (Exception e) {
            logger.info("can't read " + channelName + " " + key + " " + e.getMessage());
            return null;
        }
    }

    @Override
    public void initializeChannel(ChannelConfiguration configuration) {
        contentKeyMap.put(configuration.getName(), new SequenceContentKey(SequenceContentKey.START_VALUE));
    }

    @Override
    public Optional<ContentKey> getKey(String id) {
        return SequenceContentKey.fromString(id);
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime dateTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void delete(String channelName) {
        contentKeyMap.remove(channelName);
        contentListMultimap.removeAll(channelName);
    }

    @Override
    public void updateChannel(ChannelConfiguration configuration) { }


    @Override
    public void writeIndex(String channelName, DateTime dateTime, ContentKey key) {

    }

    @Override
    public void writeIndices(String channelName, String dateTime, List<String> keys) {

    }
}
