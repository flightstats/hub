package com.flightstats.hub.dao;

import com.flightstats.hub.cluster.LongValue;
import com.flightstats.hub.model.ContentKey;
import com.google.inject.Inject;

public class SequenceLastUpdatedDao implements LastUpdatedDao {
    private LongValue longValue;

    @Inject
    public SequenceLastUpdatedDao(LongValue longValue) {
        this.longValue = longValue;
    }

    @Override
    public void update(String channelName, ContentKey key) {
        setLastUpdateKey(channelName, key);
    }

    public void initialize(String channelName) {
        longValue.initialize(getPath(channelName), ContentKey.START_VALUE);
    }

    private void setLastUpdateKey(String channelName, ContentKey key) {
        longValue.updateIncrease(key.getSequence(), getPath(channelName));
    }

    @Override
    public ContentKey getLastUpdated(final String channelName) {
        return new ContentKey(getLongValue(channelName));
    }

    public long getLongValue(String channelName) {
        return longValue.get(getPath(channelName), ContentKey.START_VALUE);
    }

    @Override
    public void delete(String channelName) {
        longValue.delete(getPath(channelName));
    }

    public String getPath(String channelName) {
        return "/lastUpdated/" + channelName;
    }

}
