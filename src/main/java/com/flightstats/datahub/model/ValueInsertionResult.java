package com.flightstats.datahub.model;

import java.util.Date;

public class ValueInsertionResult {

    private final DataHubKey key;

    public ValueInsertionResult(DataHubKey key) {
        this.key = key;
    }

    public DataHubKey getKey() {
        return key;
    }

    public Date getDate() {
        return key.getDate();

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
