package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;

public interface ClusterWriteSpoke {
    boolean insertToWriteCluster(String path, byte[] payload, String spokeApi, String channel);
    Content getFromWriteCluster(String path, ContentKey key);
    boolean deleteFromWriteCluster(String path) throws Exception;
    QueryResult readTimeBucketFromWriteCluster(String channel, String timePath) throws InterruptedException;
}
