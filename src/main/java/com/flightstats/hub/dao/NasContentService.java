package com.flightstats.hub.dao;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.model.*;
import com.flightstats.hub.spoke.FileSpokeStore;
import com.flightstats.hub.spoke.SpokeMarshaller;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.TreeSet;

public class NasContentService implements ContentService {
    private final static Logger logger = LoggerFactory.getLogger(NasContentService.class);

    private final FileSpokeStore fileSpokeStore;

    public NasContentService() {
        String contentPath = HubProperties.getProperty("nas.content.path", "/nas");
        logger.info("using {}", contentPath);
        fileSpokeStore = new FileSpokeStore(contentPath);
    }

    @Override
    public ContentKey insert(String channelName, Content content) {
        content.getTraces().add(new Trace("NasContentService.start"));
        try {
            byte[] payload = SpokeMarshaller.toBytes(content, false);
            content.getTraces().add(new Trace("NasContentService.marshalled"));
            ContentKey key = content.keyAndStart();
            String path = getPath(channelName, key);
            logger.trace("writing key {} to channel {}", key, channelName);
            if (!fileSpokeStore.write(path, payload)) {
                logger.warn("failed to  for " + path);
            }
            content.getTraces().add(new Trace("NasContentService.end"));
            return key;
        } catch (Exception e) {
            content.getTraces().add(new Trace("NasContentService", "error", e.getMessage()));
            logger.warn("what's up?", e);
            return null;
        }
    }

    private String getPath(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
    }

    @Override
    public Optional<Content> getValue(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        try {
            byte[] bytes = fileSpokeStore.read(path);
            if (null != bytes) {
                return Optional.of(SpokeMarshaller.toContent(bytes, key));
            }
        } catch (Exception e) {
            logger.warn("unable to get data: " + path, e);
        }
        return Optional.absent();
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery timeQuery) {
        DateTime time = TimeUtil.time(timeQuery.isStable());
        String path = timeQuery.getChannelName() + "/" + timeQuery.getUnit().format(time);
        timeQuery.getTraces().add("query by time", path);
        TreeSet<ContentKey> keySet = new TreeSet<>();
        ContentKeyUtil.convertKeyStrings(fileSpokeStore.readKeysInBucket(path), keySet);
        timeQuery.getTraces().add(timeQuery.getChannelName(), keySet);
        return keySet;
    }

    @Override
    public void delete(String channelName) {
        try {
            fileSpokeStore.delete(channelName);
        } catch (Exception e) {
            logger.warn("unable to delete channel " + channelName, e);
        }
    }

    @Override
    public Collection<ContentKey> getKeys(DirectionQuery query) {
        //todo - gfm - 3/18/15 -
        return null;
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        //todo - gfm - 3/18/15 -
        return null;
    }
}
