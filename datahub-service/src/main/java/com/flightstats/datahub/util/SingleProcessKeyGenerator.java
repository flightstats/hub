package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;
import com.google.inject.Inject;

import java.util.Date;

/**
 * A thread-safe key generator implementation.  Is not compatible with cluster configurations.
 * todo - gfm - 11/4/13 - can we kill this?
 */
public class SingleProcessKeyGenerator implements DataHubKeyGenerator {

    private final TimeProvider timeProvider;
    private final Object mutex = new Object();

    private Date lastDate = new Date(Long.MIN_VALUE);
    private short sequence;

    @Inject
    public SingleProcessKeyGenerator(TimeProvider timeProvider) {
        this.timeProvider = timeProvider;
    }

    @Override
    public DataHubKey newKey(String channelName) {
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
