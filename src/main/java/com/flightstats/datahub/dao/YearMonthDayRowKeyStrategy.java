package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;

import java.text.SimpleDateFormat;

public class YearMonthDayRowKeyStrategy implements RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> {

    @Override
    public String buildKey(String channelName, DataHubKey dataHubKey) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        return format.format(dataHubKey.getDate());
    }
}
