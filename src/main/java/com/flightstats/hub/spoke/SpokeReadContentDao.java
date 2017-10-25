package com.flightstats.hub.spoke;

import com.flightstats.hub.cluster.Cluster;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.FailedQueryException;
import com.flightstats.hub.exception.FailedWriteException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class SpokeReadContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(SpokeReadContentDao.class);

    @Inject
    private RemoteSpokeStore spokeStore;

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        throw new NotImplementedException("SpokeReadContentDao.insert not implemented");
    }

    @Override
    public SortedSet<ContentKey> insert(BulkContent bulkContent) throws Exception {
        Traces traces = ActiveTraces.getLocal();
        traces.add("SpokeReadContentDao.writeBulk");
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
            traces.add("SpokeReadContentDao.writeBulk marshalled");

            logger.trace("writing items {} to channel {}", items.size(), channelName);
            if (!spokeStore.newInsert(SpokeStore.WRITE, channelName, baos.toByteArray(), Cluster.getLocalServer(), ActiveTraces.getLocal(), "bulkKey", channelName)) {
                throw new FailedWriteException("unable to write bulk to spoke " + channelName);
            }
            traces.add("SpokeReadContentDao.writeBulk completed", keys);
            return keys;
        } catch (ContentTooLargeException e) {
            logger.info("content too large for channel " + channelName);
            throw e;
        } catch (Exception e) {
            traces.add("SpokeReadContentDao", "error", e.getMessage());
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
        traces.add("SpokeReadContentDao.read");
        try {
            return spokeStore.newGet(SpokeStore.READ, path, key);
        } catch (Exception e) {
            logger.warn("unable to get data: " + path, e);
            return null;
        } finally {
            traces.add("SpokeReadContentDao.read completed");
        }
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        logger.trace("query by time {} ", query);
        ActiveTraces.getLocal().add("SpokeReadContentDao.queryByTime", query);
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
        ActiveTraces.getLocal().add("SpokeReadContentDao.queryByTime completed", contentKeys);
        return contentKeys;
    }

    private SortedSet<ContentKey> queryByTimeKeys(TimeQuery query) {
        try {
            String timePath = query.getUnit().format(query.getStartTime());
            QueryResult queryResult = spokeStore.newReadTimeBucket(SpokeStore.READ, query.getChannelName(), timePath);
            ActiveTraces.getLocal().add("spoke query result", queryResult);
            if (!queryResult.hadSuccess()) {
                QueryResult retryResult = spokeStore.newReadTimeBucket(SpokeStore.READ, query.getChannelName(), timePath);
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
        throw new NotImplementedException("SpokeReadContentDao.query not implemented");
    }

    @Override
    public void delete(String channelName) {
        try {
            long start = System.currentTimeMillis();
            spokeStore.newDelete(SpokeStore.READ, channelName);
            long elapsed = System.currentTimeMillis() - start;
            logger.debug("SpokeReadContentDao.delete({}) took {} ms", channelName, elapsed);
        } catch (Exception e) {
            logger.warn("unable to delete " + channelName, e);
        }
    }

    @Override
    public void initialize() {
        // do nothing
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        throw new NotImplementedException("SpokeReadContentDao.getLatest not implemented");
    }

    @Override
    public void deleteBefore(String channelName, ContentKey limitKey) {
        throw new NotImplementedException("SpokeReadContentDao.deleteBefore not implemented");
    }

}
