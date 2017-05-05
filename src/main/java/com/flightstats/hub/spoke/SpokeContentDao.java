package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.FailedQueryException;
import com.flightstats.hub.exception.FailedWriteException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.*;

/**
 * This is the entry point in the Hub's storage system, Spoke.
 * <p>
 * It is called in-process on the originating Hub server, and this class will
 * call the registered Spoke servers in the cluster.
 */
public class SpokeContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(SpokeContentDao.class);

    @Inject
    private RemoteSpokeStore spokeStore;

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        ContentKey key = content.getContentKey().get();
        String path = getPath(channelName, key);
        if (!spokeStore.insert(path, content.getData(), "payload", channelName)) {
            throw new FailedWriteException("unable to write to spoke " + path);
        }
        return key;
    }

    @Override
    public SortedSet<ContentKey> insert(BulkContent bulkContent) throws Exception {
        Traces traces = ActiveTraces.getLocal();
        traces.add("SpokeContentDao.writeBulk");
        String channelName = bulkContent.getChannel();
        try {
            SortedSet<ContentKey> keys = new TreeSet<>();
            List<Content> items = bulkContent.getItems();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(baos);
            stream.writeInt(items.size());
            logger.debug("writing {} items to master {}", items.size(), bulkContent.getMasterKey());
            for (Content content : items) {
                content.packageStream();
                String itemKey = content.getContentKey().get().toUrl();
                stream.writeInt(itemKey.length());
                stream.write(itemKey.getBytes());
                stream.writeInt(content.getData().length);
                stream.write(content.getData());
                keys.add(content.getContentKey().get());
            }
            stream.flush();
            traces.add("SpokeContentDao.writeBulk marshalled");

            logger.trace("writing items {} to channel {}", items.size(), channelName);
            if (!spokeStore.insert(channelName, baos.toByteArray(), "bulkKey", channelName)) {
                throw new FailedWriteException("unable to write bulk to spoke " + channelName);
            }
            traces.add("SpokeContentDao.writeBulk completed", keys);
            return keys;
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
    public Content get(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        Traces traces = ActiveTraces.getLocal();
        traces.add("SpokeContentDao.read");
        try {
            return spokeStore.get(channelName, path, key);
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
        SortedSet<ContentKey> contentKeys;
        if (query.getLimitKey() == null) {
            contentKeys = queryByTimeKeys(query);
        } else {
            contentKeys = queryByTimeKeys(query);
            while (query.getStartTime().isBefore(query.getLimitKey().getTime())) {
                query = query.withStartTime(query.getStartTime().plus(query.getUnit().getDuration()));
                contentKeys.addAll(queryByTimeKeys(query));
            }
        }
        ActiveTraces.getLocal().add("SpokeContentDao.queryByTime completed", contentKeys);
        return contentKeys;
    }

    private SortedSet<ContentKey> queryByTimeKeys(TimeQuery query) {
        try {
            QueryResult queryResult = spokeStore.readTimeBucket(query);
            ActiveTraces.getLocal().add("spoke query result", queryResult);
            if (!queryResult.hadSuccess()) {
                QueryResult retryResult = spokeStore.readTimeBucket(query);
                ActiveTraces.getLocal().add("spoke query retryResult", retryResult);
                if (!retryResult.hadSuccess()) {
                    ActiveTraces.getLocal().log(logger);
                    throw new FailedQueryException("unable to execute time query " + query + " " + queryResult);
                }
                queryResult.getContentKeys().addAll(retryResult.getContentKeys());
            }
            ActiveTraces.getLocal().add("spoke query by time", queryResult.getContentKeys());
            return queryResult.getContentKeys();
        } catch (FailedQueryException rethrow) {
            throw rethrow;
        } catch (Exception e) {
            logger.warn("what happened? " + query, e);
        }
        return Collections.emptySortedSet();
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        int ttlMinutes = HubProperties.getSpokeTtl();
        DateTime spokeTtlTime = TimeUtil.BIG_BANG;
        if (HubProperties.getProperty("spoke.enforceTTL", true)) {
            spokeTtlTime = query.getChannelStable().minusMinutes(ttlMinutes);
            if (query.getChannelConfig().isLive()) {
                if (query.getStartKey().getTime().isBefore(spokeTtlTime)) {
                    query = query.withStartKey(new ContentKey(spokeTtlTime, "0"));
                }
            } else {
                spokeTtlTime = query.getChannelStable().minusMinutes(ttlMinutes * 2);
            }
        }
        ActiveTraces.getLocal().add("SpokeContentDao.query ", query, spokeTtlTime);
        SortedSet<ContentKey> contentKeys = Collections.emptySortedSet();
        if (query.isNext()) {
            try {
                contentKeys = spokeStore.getNext(query.getChannelName(), query.getCount(), query.getStartKey());
            } catch (InterruptedException e) {
                logger.warn("what happened? " + query, e);
            }
        } else {
            ContentKey startKey = query.getStartKey();
            DateTime startTime = startKey.getTime();
            contentKeys = new TreeSet<>();
            while (contentKeys.size() < query.getCount()
                    && startTime.isAfter(spokeTtlTime.minusHours(1))
                    && startTime.isBefore(query.getChannelStable().plusHours(1))) {
                TimeQuery timeQuery = query.convert(TimeUtil.Unit.HOURS)
                        .startTime(startTime)
                        .build();
                SortedSet<ContentKey> queryByTime = queryByTime(timeQuery);
                queryByTime.addAll(contentKeys);
                Set<ContentKey> filtered = ContentKeyUtil.filter(queryByTime, query);
                contentKeys.addAll(filtered);
                startTime = startTime.minusHours(1);
            }
        }
        ActiveTraces.getLocal().add("SpokeContentDao.query completed", contentKeys);
        return contentKeys;
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
