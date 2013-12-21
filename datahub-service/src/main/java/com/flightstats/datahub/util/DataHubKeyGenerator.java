package com.flightstats.datahub.util;

import com.flightstats.datahub.model.SequenceDataHubKey;

public interface DataHubKeyGenerator {

    SequenceDataHubKey newKey(String channelName);

    void seedChannel(String channelName);
}
