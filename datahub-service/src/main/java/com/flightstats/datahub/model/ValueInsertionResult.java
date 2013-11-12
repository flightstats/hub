package com.flightstats.datahub.model;

import java.util.Date;

public class ValueInsertionResult {

    private final DataHubKey key;
    private String rowKey;
    private final Date date = new Date();

    public ValueInsertionResult(DataHubKey key) {
        this.key = key;
    }

    public ValueInsertionResult(DataHubKey key, String rowKey) {
        this(key);
        this.rowKey = rowKey;
    }

    public DataHubKey getKey() {
        return key;
    }

    public Date getDate() {
        return date;
    }

    public String getRowKey() {
        return rowKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ValueInsertionResult that = (ValueInsertionResult) o;

        if (!key.equals(that.key)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
