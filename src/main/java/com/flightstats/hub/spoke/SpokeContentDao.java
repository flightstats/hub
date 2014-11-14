package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * This is the entry point in the Hub's storage system, Spoke.
 * <p>
 * It is called in-process on the originating Hub server, and this class will
 * call the Spoke servers in the cluster.
 */
public class SpokeContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(SpokeContentDao.class);

    //todo - gfm - 11/13/14 - where should this formatting live?  thick client or server?
    private final static DateTimeFormatter pathFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ssSSS").withZoneUTC();
    private final RemoteSpokeStore spokeStore;

    @Inject
    public SpokeContentDao(RemoteSpokeStore spokeStore) {
        this.spokeStore = spokeStore;
    }

    @Override
    public ContentKey write(String channelName, Content content) {
        if (content.isNewContent()) {
            content.setContentKey(new ContentKey());
        } else {
            //todo - gfm - 10/31/14 - how should replication be handled?
        }
        try {
            ContentKey key = content.getContentKey().get();
            spokeStore.write(getPath(channelName, key), SpokeMarshaller.toBytes(content));
            return key;
        } catch (Exception e) {
            logger.warn("what's up?", e);
            return null;
        }
    }

    private String getPath(String channelName, ContentKey key) {
        return channelName + "/" + key.toString(pathFormatter);
    }

    @Override
    public Content read(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        try {
            return spokeStore.read(path, key);
        } catch (Exception e) {
            logger.warn("unable to get data: " + path, e);
            return null;
        }
    }

    @Override
    public void initializeChannel(ChannelConfiguration configuration) {
        //todo - gfm - 11/11/14 - do anything?
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, DateTime startTime, DateTime endTime) {

        return null;
    }

    @Override
    public Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count) {
        return null;
    }

    @Override
    public void delete(String channelName) {
        try {
            spokeStore.delete(channelName);
        } catch (Exception e) {
            logger.warn("unable to delete " + channelName, e);
        }

    }
}
