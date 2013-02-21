package com.flightstats.datahub.model;

import java.util.Date;

public class DataHubKey {

    private final Date date;
    private final short sequence;

    public DataHubKey(Date date, short sequence) {
        this.date = date;
        this.sequence = sequence;
    }

    public Date getDate() {
        return date;
    }

    public short getSequence() {
        return sequence;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        DataHubKey that = (DataHubKey) o;

        if (sequence != that.sequence) {
            return false;
        }
        if (date != null ? !date.equals(that.date) : that.date != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + sequence;
        return result;
    }

    @Override
    public String toString() {
        return "DataHubKey{" +
                "date=" + date +
                ", sequence=" + sequence +
                '}';
    }
}
