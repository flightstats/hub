package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;

/**
 *
 */
public class SequenceRowKeyStrategy implements RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> {

    public static final long INCREMENT = 1000;

    @Override
    public String buildKey(String channelName, DataHubKey dataHubKey) {
        return concat(channelName, (dataHubKey.getSequence() / INCREMENT));
    }

    private String concat(String channelName, long value) {
        return channelName + ":" + value;
    }

}
