package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This is the entry point in the Hub's storage system, Spoke.
 * <p>
 * It is called in-process on the originating Hub server, and this class will
 * call the Spoke servers in the cluster.
 */
public class SpokeContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(SpokeContentDao.class);

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
            String path = getPath(channelName, key);
            if (!spokeStore.write(path, SpokeMarshaller.toBytes(content))) {
                logger.warn("failed to write to all for " + path);
            }
            return key;
        } catch (Exception e) {
            logger.warn("what's up?", e);
            return null;
        }
    }

    private String getPath(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
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
    public Collection<ContentKey> queryByTime(String channelName, DateTime startTime, TimeUtil.Unit unit) {
        String timePath = unit.format(startTime);
        try {
            // TODO bc 11/17/14: limit this to day, hour, minute, second, millis
            List<ContentKey> keys = new ArrayList<>();
            Collection<String> strings = spokeStore.readTimeBucket(channelName + "/" + timePath);
            for (String string : strings) {
                keys.add(ContentKey.fromUrl(string).get());
            }
            return keys;
        } catch (Exception e) {
            logger.warn("huh?", e);
            e.printStackTrace();
        }
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
