package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import javax.inject.Inject;

public class ReadOnlyClusterSpokeStore implements ClusterWriteSpoke {
    private final ClusterWriteSpoke writeClusterDelegate;

    @Inject
    public ReadOnlyClusterSpokeStore(ClusterWriteSpoke writeClusterDelegate) {
        this.writeClusterDelegate = writeClusterDelegate;
    }

    @Override
    public boolean insertToWriteCluster(String path, byte[] payload, String spokeApi, String channel) {
        throw new UnsupportedOperationException("Unable to insert to remote spoke store from a r/o node " + channel);
    }

    @Override
    public Content getFromWriteCluster(String path, ContentKey key) {
        return writeClusterDelegate.getFromWriteCluster(path, key);
    }

    @Override
    public boolean deleteFromWriteCluster(String path) {
        throw new UnsupportedOperationException("Unable to delete from the remote spoke store from a r/o node " + path);
    }

    @Override
    public QueryResult readTimeBucketFromWriteCluster(String channel, String timePath) throws InterruptedException {
        return writeClusterDelegate.readTimeBucketFromWriteCluster(channel, timePath);
    }

}
