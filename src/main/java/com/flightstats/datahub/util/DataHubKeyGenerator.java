package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;

import java.util.Date;

public class DataHubKeyGenerator {

    private final TimeProvider timeProvider;
    private final Object mutex = new Object();

    private Date lastDate = new Date(Long.MIN_VALUE);
    private short sequence;

    public DataHubKeyGenerator(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    public DataHubKey newKey() {
        Date date = timeProvider.getDate();
        synchronized (mutex) {
            if (date.compareTo(lastDate) <= 0) {  //in the same millisecond or before in time
                return new DataHubKey(lastDate, ++sequence);
            }
            sequence = 0;
            lastDate = date;
            return new DataHubKey(date, sequence);
        }
    }
}
