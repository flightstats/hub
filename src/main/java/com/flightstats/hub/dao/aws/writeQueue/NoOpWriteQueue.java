package com.flightstats.hub.dao.aws.writeQueue;

import com.flightstats.hub.model.ChannelContentKey;

public class NoOpWriteQueue implements WriteQueue {

    @Override
    public boolean add(ChannelContentKey key) {
        return true;
    }

}
