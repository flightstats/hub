package com.flightstats.hub.dao.aws.writeQueue;

import com.flightstats.hub.model.ChannelContentKey;

public interface WriteQueue {
    boolean add(ChannelContentKey key);

}
