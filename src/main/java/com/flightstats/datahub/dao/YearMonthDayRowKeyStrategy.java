package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.flightstats.datahub.model.DataHubKey;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class YearMonthDayRowKeyStrategy implements RowKeyStrategy<String, DataHubKey, DataHubCompositeValue> {

    private final static DateTimeFormatter formatter = ISODateTimeFormat.basicDate().withZoneUTC();

    @Override
    public String buildKey(String channelName, DataHubKey dataHubKey) {
        return formatter.print(dataHubKey.getDate().getTime());
    }

	@Override
	public String nextKey(String channelName, String currentRowKey) {
		DateTime date = formatter.parseDateTime(currentRowKey);
		date = date.plusDays(1);
		return formatter.print(date.getMillis());
	}

	@Override
	public String prevKey(String channelName, String currentRowKey) {
		DateTime date = formatter.parseDateTime(currentRowKey);
		date = date.minusDays(1);
		return formatter.print(date.getMillis());
	}
}
