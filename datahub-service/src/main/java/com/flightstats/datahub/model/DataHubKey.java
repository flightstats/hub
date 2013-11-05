package com.flightstats.datahub.model;

import com.google.common.base.Optional;

import java.io.Serializable;
import java.util.Date;

public class DataHubKey implements Serializable {
    private static final long serialVersionUID = 1L;
    //todo - gfm - 11/4/13 - figure out how MIN_SEQUENCE works
    private static final long MIN_SEQUENCE = 0;
    private Date date;
    private final long sequence;

    /**
     * @deprecated
     */
    public DataHubKey(Date date, long sequence) {
        this.date = date;
        this.sequence = sequence;
    }

    public DataHubKey(long sequence) {
        //todo - gfm - 11/4/13 - how to enforce MIN_SEQUENCE?
        this.sequence = sequence;
    }

    /**
     * @deprecated
     */
    public Date getDate() {
        return date;
    }

    public long getSequence() {
        return sequence;
    }

    public Optional<DataHubKey> getNext() {
        return Optional.of(new DataHubKey(getSequence() + 1));
    }

    public Optional<DataHubKey> getPrevious() {
        if (getSequence() <= MIN_SEQUENCE) {
            return Optional.absent();
        }
        return Optional.of(new DataHubKey(getSequence() - 1));
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
        //todo - gfm - 11/4/13 - eventually remove date
        if (date != null ? !date.equals(that.date) : that.date != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        //todo - gfm - 11/4/13 - eventually remove date
        int result = date != null ? date.hashCode() : 0;
        result = 31 * result + (int) (sequence ^ (sequence >>> 32));
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
