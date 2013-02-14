package com.flightstats.datahub.dao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class YearMonthDayRowKeyStrategy implements RowKeyStrategy<String, UUID, byte[]> {

    private final HectorFactoryWrapper hector;

    public YearMonthDayRowKeyStrategy(HectorFactoryWrapper hector) {
        this.hector = hector;
    }

    @Override
    public String buildKey(String channelName, UUID columnName, byte[] data) {
        long timestamp = hector.getTimeFromUUID(columnName);
        SimpleDateFormat format = new SimpleDateFormat("YMMdd");
        return format.format(new Date(timestamp));
    }
}
