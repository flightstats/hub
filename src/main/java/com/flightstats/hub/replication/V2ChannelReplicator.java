package com.flightstats.hub.replication;

import com.flightstats.hub.model.ChannelConfiguration;

public class V2ChannelReplicator implements ChannelReplicator {

    //todo - gfm - 1/23/15 - this needs to create a group callback for the channel.


    @Override
    public ChannelConfiguration getChannel() {
        return null;
    }

    @Override
    public void exit() {
        //todo - gfm - 1/23/15 - delete group callback
    }
}
