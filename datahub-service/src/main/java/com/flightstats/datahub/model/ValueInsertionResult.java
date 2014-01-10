package com.flightstats.datahub.model;

import java.util.Date;

public class ValueInsertionResult {

    private final ContentKey key;
    private final Date date;

    public ValueInsertionResult(ContentKey key, Date date) {
        this.key = key;
        this.date = date;
    }

    public ContentKey getKey() {
        return key;
    }

    public Date getDate() {
        return date;
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
