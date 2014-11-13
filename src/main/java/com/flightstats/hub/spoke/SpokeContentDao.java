package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

/**
 * This is the entry point in the Hub's storage system, Spoke.
 * <p>
 * It is called in-process on the originating Hub server, and this class will
 * call the other Spoke servers in the cluster.
 * <p>
 * Eventually, it may make sense to pull this out as a separate system.
 */
public class SpokeContentDao implements ContentDao {


    private final static Logger logger = LoggerFactory.getLogger(SpokeContentDao.class);

    //todo - gfm - 11/13/14 - where should this formatting live?
    private static final DateTimeFormatter pathFormatter = DateTimeFormat.forPattern("yyyy/MM/dd/HH/mm/ssSSS").withZoneUTC();
    private final SpokeFileStore spokeFileStore;

    @Inject
    public SpokeContentDao(SpokeFileStore spokeFileStore) {
        this.spokeFileStore = spokeFileStore;
    }

    @Override
    public InsertedContentKey write(String channelName, Content content, long ttlDays) {
        if (content.isNewContent()) {
            content.setContentKey(new ContentKey());
        } else {
            //todo - gfm - 10/31/14 - how should replication be handled?
        }
        try {
            ContentKey key = content.getContentKey().get();
            spokeFileStore.write(getPath(channelName, key), SpokeMarshaller.toBytes(content));

            //todo - gfm - 11/11/14 - send async to other stores, wait for success
            return new InsertedContentKey(key, new DateTime(key.getMillis()).toDate());
        } catch (IOException e) {
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
            byte[] read = spokeFileStore.read(path);
            return SpokeMarshaller.toContent(read, key);
            //todo - gfm - 11/11/14 - try next store
            //todo - gfm - 11/11/14 - try 3rd store
        } catch (IOException e) {
            logger.warn("unable to get data" + path, e);
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

    }
}
