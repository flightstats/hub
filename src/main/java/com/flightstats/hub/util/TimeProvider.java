package com.flightstats.hub.util;

import org.joda.time.DateTime;

import java.util.Date;

/**
 * This class is primarily geared at helping isolate date creation for tests, but
 * could also be leveraged in the future to provide different time mechanisms.
 */
public class TimeProvider {

    public Date getDate() {
        return new Date();
    }

    public DateTime getDateTime() {
        return new DateTime();
    }

    public long getMillis(){
        return System.currentTimeMillis();
    }
}
