package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;

public interface LocalReadSpoke {
    boolean insertToLocalReadStore(String path, byte[] payload, Traces traces, String spokeApi, String channel);
    Content getFromLocalReadStore(String path, ContentKey key);
    boolean deleteFromLocalReadStore(String path) throws Exception;
    QueryResult readTimeBucketFromLocalReadStore(String channel, String timePath) throws InterruptedException;
}