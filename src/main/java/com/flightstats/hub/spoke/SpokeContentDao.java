package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.FailedWriteException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.time.TimeService;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
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

    @Inject
    private RemoteSpokeStore spokeStore;
    @Inject
    private TimeService timeService;

    private final int ttlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);

    @Override
    public ContentKey write(String channelName, Content content) throws Exception {
        Traces traces = ActiveTraces.getLocal();
        traces.add("SpokeContentDao.write");
        try {
            byte[] payload = SpokeMarshaller.toBytes(content);
            traces.add("SpokeContentDao.write marshalled");
            ContentKey key = content.keyAndStart(timeService.getNow());
            String path = getPath(channelName, key);
            logger.trace("writing key {} to channel {}", key, channelName);
            if (!spokeStore.write(path, payload, content)) {
                throw new FailedWriteException("unable to write to spoke " + path);
            }
            traces.add("SpokeContentDao.write completed", key);
            return key;
        } catch (ContentTooLargeException e) {
            logger.info("content too large for channel " + channelName);
            throw e;
        } catch (Exception e) {
            traces.add("SpokeContentDao", "error", e.getMessage());
            logger.error("unable to write " + channelName, e);
            throw e;
        }
    }

    private String getPath(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
    }

    @Override
    public Content read(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        Traces traces = ActiveTraces.getLocal();
        traces.add("SpokeContentDao.read");
        try {
            return spokeStore.read(path, key);
        } catch (Exception e) {
            logger.warn("unable to get data: " + path, e);
            return null;
        } finally {
            traces.add("SpokeContentDao.read completed");
        }
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        String path = getPath(channel, limitKey);
        logger.trace("latest {} {}", channel, path);
        traces.add("SpokeContentDao.latest", channel, path);
        try {
            Optional<ContentKey> key = spokeStore.getLatest(channel, path, traces);
            traces.add("SpokeContentDao.latest", key);
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
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        logger.trace("query by time {} ", query);
        ActiveTraces.getLocal().add("SpokeContentDao.queryByTime", query);
        SortedSet<ContentKey> contentKeys = Collections.emptySortedSet();
        if (query.getEndTime() == null) {
            contentKeys = queryByTimeKeys(query);
        } else {
            contentKeys = queryByTimeKeys(query);
            while (query.getStartTime().isBefore(query.getEndTime())) {
                query = query.withStartTime(query.getStartTime().plus(query.getUnit().getDuration()));
                contentKeys.addAll(queryByTimeKeys(query));
            }
        }
        ActiveTraces.getLocal().add("SpokeContentDao.queryByTime completed", contentKeys);
        return contentKeys;
    }

    private SortedSet<ContentKey> queryByTimeKeys(TimeQuery query) {
        try {
            String timePath = query.getUnit().format(query.getStartTime());
            SortedSet<ContentKey> keys = spokeStore.readTimeBucket(query.getChannelName(), timePath);
            ActiveTraces.getLocal().add("spoke query by time", keys);
            return keys;
        } catch (Exception e) {
            logger.warn("what happened? " + query, e);
        }
        return Collections.emptySortedSet();
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        ActiveTraces.getLocal().add("SpokeContentDao.query", query);
        DateTime ttlTime = TimeUtil.now().minusMinutes(ttlMinutes);
        if (query.getContentKey().getTime().isBefore(ttlTime)) {
            query = query.withContentKey(new ContentKey(ttlTime, "0"));
        }
        SortedSet<ContentKey> contentKeys = Collections.emptySortedSet();
        if (query.isNext()) {
            try {
                contentKeys = spokeStore.getNext(query.getChannelName(), query.getCount(), query.getContentKey().toUrl());
            } catch (InterruptedException e) {
                logger.warn("what happened? " + query, e);
            }
        } else {
            ContentKey startKey = query.getContentKey();
            DateTime startTime = startKey.getTime();
            DateTime endTime = TimeUtil.time(query.isStable());
            contentKeys = new TreeSet<>();
            while (contentKeys.size() < query.getCount()
                    && startTime.isAfter(ttlTime.minusHours(1))
                    && startTime.isBefore(endTime.plusHours(1))) {
                query(query, contentKeys, startKey, startTime, ttlTime, TimeUtil.Unit.HOURS);
                startTime = startTime.minusHours(1);
            }
        }
        ActiveTraces.getLocal().add("SpokeContentDao.query completed", contentKeys);
        return contentKeys;
    }

    private void query(DirectionQuery query, SortedSet<ContentKey> orderedKeys, ContentKey startKey, DateTime startTime, DateTime ttlTime, TimeUtil.Unit unit) {
        SortedSet<ContentKey> queryByTime = queryByTime(query.convert(startTime, unit));
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
