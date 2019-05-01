package com.flightstats.hub.cluster;

import org.apache.curator.framework.CuratorFramework;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;

class DateTimeValue {
    private final LongValue longValue;

    @Inject
    public DateTimeValue(CuratorFramework curator) {
        longValue = new LongValue(curator);
    }

    public void initialize(String path, DateTime defaultValue) {
        longValue.initialize(path, defaultValue.getMillis());
    }

    public DateTime get(String path, DateTime defaultValue) {
        return new DateTime(longValue.get(path, defaultValue.getMillis()), DateTimeZone.UTC);
    }

    public void update(String path, DateTime value) {
        longValue.updateIncrease(value.getMillis(), path);
    }

    public void delete(String path) {
        longValue.delete(path);
    }

}
