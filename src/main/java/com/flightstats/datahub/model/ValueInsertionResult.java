package com.flightstats.datahub.model;

import java.util.Date;
import java.util.UUID;

public class ValueInsertionResult {

    private final UUID id;

    private final Date date;

    public ValueInsertionResult(UUID id, Date date) {
        this.id = id;
        this.date = date;
    }

    public UUID getId() {
        return id;
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

        if (date != null ? !date.equals(that.date) : that.date != null) {
            return false;
        }
        if (id != null ? !id.equals(that.id) : that.id != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (date != null ? date.hashCode() : 0);
        return result;
    }
}
