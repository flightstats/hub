package com.flightstats.hub.dao.aws;

import com.flightstats.hub.model.ChannelContentKey;

public interface BackingStoreWriteQueue extends AutoCloseable {
    boolean add(ChannelContentKey key);

}
