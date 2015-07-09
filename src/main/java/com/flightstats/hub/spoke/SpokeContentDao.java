package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This is the entry point in the Hub's storage system, Spoke.
 * <p>
 * It is called in-process on the originating Hub server, and this class will
 * call the Spoke servers in the cluster.
 */
public class SpokeContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(SpokeContentDao.class);

    private final RemoteSpokeStore spokeStore;

    private final int ttlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);

    @Inject
    public SpokeContentDao(RemoteSpokeStore spokeStore) {
        this.spokeStore = spokeStore;
    }

    @Override
    public ContentKey write(String channelName, Content content) throws Exception {
        content.getTraces().add(new Trace("SpokeContentDao.start"));
        try {
            byte[] payload = SpokeMarshaller.toBytes(content);
            content.getTraces().add(new Trace("SpokeContentDao.marshalled"));
            ContentKey key = content.keyAndStart();
            String path = getPath(channelName, key);
            logger.trace("writing key {} to channel {}", key, channelName);
            if (!spokeStore.write(path, payload, content)) {
                logger.warn("failed to write to all for " + path);
            }
            content.getTraces().add(new Trace("SpokeContentDao.end"));
            return key;
        } catch (ContentTooLargeException e) {
            logger.info("content too large for channel " + channelName);
            throw e;
        } catch (Exception e) {
            content.getTraces().add(new Trace("SpokeContentDao", "error", e.getMessage()));
            logger.warn("unable to write " + channelName, e);
            throw e;
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
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        String path = getPath(channel, limitKey);
        logger.trace("latest {} {}", channel, path);
        traces.add("spoke latest", channel, path);
        try {
            Optional<ContentKey> key = spokeStore.getLatest(channel, path, traces);
            traces.add("spoke query by time", key);
            return key;
        } catch (Exception e) {
            logger.warn("what happened? " + channel, e);
        }
        return Optional.absent();
    }

    @Override
    public void deleteBefore(String channelName, ContentKey limitKey) {
        throw new UnsupportedOperationException("deleteBefore is not supported");
    }

    @Override
    public SortedSet<ContentKey> queryByTime(String channelName, DateTime startTime, TimeUtil.Unit unit, Traces traces) {
        logger.trace("query by time {} {} {}", channelName, startTime, unit);
        traces.add("spoke query by time", channelName, startTime, unit);
        String timePath = unit.format(startTime);
        try {
            SortedSet<ContentKey> keys = spokeStore.readTimeBucket(channelName, timePath, traces);
            traces.add("spoke query by time", keys);
            return keys;
        } catch (Exception e) {
            logger.warn("what happened? " + channelName + " " + startTime + " " + unit, e);
        }
        return new TreeSet<>();
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        SortedSet<ContentKey> orderedKeys = new TreeSet<>();
        DateTime ttlTime = TimeUtil.now().minusMinutes(ttlMinutes);
        if (query.getContentKey().getTime().isBefore(ttlTime)) {
            query = query.withContentKey(new ContentKey(ttlTime, "0"));
        }
        ContentKey startKey = query.getContentKey();
        DateTime startTime = startKey.getTime();
        while (orderedKeys.size() < query.getCount()
                && startTime.isAfter(ttlTime.minusHours(1))
                && startTime.isBefore(TimeUtil.time(query.isStable()).plusHours(1))) {
            query(query, orderedKeys, startKey, startTime, ttlTime);
            startTime = query.isNext() ? startTime.plusHours(1) : startTime.minusHours(1);
        }
        return orderedKeys;
    }

    private void query(DirectionQuery query, SortedSet<ContentKey> orderedKeys, ContentKey startKey, DateTime startTime, DateTime ttlTime) {
        SortedSet<ContentKey> queryByTime = queryByTime(query.getChannelName(), startTime, TimeUtil.Unit.HOURS, query.getTraces());
        queryByTime.addAll(orderedKeys);
        Set<ContentKey> filtered = ContentKeyUtil.filter(queryByTime, query.getContentKey(), ttlTime, query.getCount(), query.isNext(), query.isStable());
        orderedKeys.addAll(filtered);
    }

    @Override
    public void delete(String channelName) {
        try {
            spokeStore.delete(channelName);
        } catch (Exception e) {
            logger.warn("unable to delete " + channelName, e);
        }
    }

    @Override
    public void initialize() {
        //do anything?
    }
}
