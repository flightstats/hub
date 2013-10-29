package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import org.joda.time.DateTime;
import org.joda.time.DateTimeFieldType;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

public class ChannelHourRowKeyStrategy implements RowKeyStrategy<String, DataHubKey, DataHubCompositeValue>{

    private final static DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendYear(4, 4)
            .appendFixedDecimal(DateTimeFieldType.monthOfYear(), 2)
            .appendFixedDecimal(DateTimeFieldType.dayOfMonth(), 2)
            .appendFixedDecimal(DateTimeFieldType.hourOfDay(), 2)
            .toFormatter();

    @Override
    public String buildKey(String channelName, DataHubKey dataHubKey) {
        return addPrefix(channelName, formatter.print(dataHubKey.getDate().getTime()));
    }

    @Override
    public String nextKey(String channelName, String currentRowKey) {
        String datePart = stripPrefix(channelName, currentRowKey);
        DateTime date = formatter.parseDateTime(datePart);
        date = date.plusHours(1);
        return addPrefix(channelName, formatter.print(date.getMillis()));
    }

    @Override
    public String prevKey(String channelName, String currentRowKey) {
        String datePart = stripPrefix(channelName, currentRowKey);
        DateTime date = formatter.parseDateTime(datePart);
        date = date.minusHours(1);
        return addPrefix(channelName, formatter.print(date.getMillis()));
    }

    private String addPrefix(String channelName, String dateString) {
        return channelName + ":" + dateString;
    }

    private String stripPrefix(String channelName, String key){
        return key.substring(channelName.length() + 1);
    }
}
