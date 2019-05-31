package com.flightstats.hub.cluster;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import static com.flightstats.hub.constant.ZookeeperNodes.LAST_COMMITTED_CONTENT_KEY;

@Slf4j
public class LatestContentCache {
    /** The latest item on the channel that is older than stable time, older than spoke's TTL, and younger than the channel's TTL. i.e. the "cached" latest */
    private final ClusterCacheDao clusterCacheDao;

    @Inject
    public LatestContentCache(ClusterCacheDao clusterCacheDao) {
        this.clusterCacheDao = clusterCacheDao;
    }

    public ContentPath getLatest(String channelName, ContentPath defValue) {
        return clusterCacheDao.get(channelName, defValue, LAST_COMMITTED_CONTENT_KEY);
    }

    public boolean isChannelEmpty(String channelName) {
        return ContentKey.NONE.equals(getLatest(channelName, ContentKey.NONE));
    }

    public void setEmpty(String channelName) {
        clusterCacheDao.set(ContentKey.NONE, channelName, LAST_COMMITTED_CONTENT_KEY);
    }

    public void setIfNewer(String channelName, ContentKey key) {
        clusterCacheDao.setIfNewer(key, channelName, LAST_COMMITTED_CONTENT_KEY);
    }

    public void deleteCache(String channelName) {
        clusterCacheDao.delete(channelName, LAST_COMMITTED_CONTENT_KEY);
    }

}