package com.flightstats.datahub.util;

import com.flightstats.datahub.model.SequenceContentKey;

public interface DataHubKeyGenerator {

    SequenceContentKey newKey(String channelName);

    void seedChannel(String channelName);
}
