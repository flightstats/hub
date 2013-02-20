package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.google.inject.Inject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class YearMonthDayRowKeyStrategy implements RowKeyStrategy<String, UUID, DataHubCompositeValue> {

    private final HectorFactoryWrapper hector;

    @Inject
    public YearMonthDayRowKeyStrategy(HectorFactoryWrapper hector) {
        this.hector = hector;
    }

    @Override
    public String buildKey(String channelName, UUID columnName) {
        long timestamp = hector.getTimeFromUUID(columnName);
        SimpleDateFormat format = new SimpleDateFormat("YMMdd");
        return format.format(new Date(timestamp));
    }
}
