package com.flightstats.hub.spoke;

import com.flightstats.hub.cluster.CuratorCluster;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.exception.FailedQueryException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.google.common.base.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.Collections;
import java.util.SortedSet;

public class SpokeReadContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(SpokeReadContentDao.class);

    private final SpokeContentDao spokeContentDao;
    private final RemoteSpokeStore spokeStore;
    private final CuratorCluster cluster;

    @Inject
    SpokeReadContentDao(SpokeContentDao spokeContentDao, RemoteSpokeStore spokeStore, @Named("SpokeCluster") CuratorCluster cluster) {
        this.spokeContentDao = spokeContentDao;
        this.spokeStore = spokeStore;
        this.cluster = cluster;
    }

    @Override
    public ContentKey insert(String channelName, Content content) {
        throw new NotImplementedException("SpokeReadContentDao.insert not implemented");
    }

    @Override
    public SortedSet<ContentKey> insert(BulkContent bulkContent) throws Exception {
        return spokeContentDao.insert(bulkContent, (baos) -> {
            String channel = bulkContent.getChannel();
            return spokeStore.insert(SpokeStore.READ, channel, baos.toByteArray(), cluster.getLocalServer(), ActiveTraces.getLocal(), "bulkKey", channel);
        });
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
            return spokeStore.get(SpokeStore.READ, path, key);
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
            QueryResult queryResult = spokeStore.readTimeBucket(SpokeStore.READ, query.getChannelName(), timePath);
            ActiveTraces.getLocal().add("spoke query result", queryResult);
            if (!queryResult.hadSuccess()) {
                QueryResult retryResult = spokeStore.readTimeBucket(SpokeStore.READ, query.getChannelName(), timePath);
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
            spokeStore.delete(SpokeStore.READ, channelName);
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
