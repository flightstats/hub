package com.flightstats.hub.cluster;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

@Slf4j
public class LatestContentCache {
    /** The latest item on the channel that is older than stable time, older than spoke's TTL, and younger than the channel's TTL. i.e. the "cached" latest */
    public static final String LAST_COMMITTED_CONTENT_KEY = "/ChannelLatestUpdated/";
    private final ClusterStateDao clusterStateDao;

    @Inject
    public LatestContentCache(ClusterStateDao clusterStateDao) {
        this.clusterStateDao = clusterStateDao;
    }

    public ContentPath getLatest(String channelName, ContentPath defValue) {
        return clusterStateDao.get(channelName, defValue, LAST_COMMITTED_CONTENT_KEY);
    }

    public void setEmpty(String channelName) {
        clusterStateDao.set(ContentKey.NONE, channelName, LAST_COMMITTED_CONTENT_KEY);
    }

    public void setIfAfter(String channelName, ContentKey key) {
        clusterStateDao.setIfAfter(key, channelName, LAST_COMMITTED_CONTENT_KEY);
    }

    public void deleteCache(String channelName) {
        clusterStateDao.delete(channelName, LAST_COMMITTED_CONTENT_KEY);
    }

}