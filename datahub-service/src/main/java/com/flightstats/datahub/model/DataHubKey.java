package com.flightstats.datahub.model;

import com.flightstats.datahub.model.exception.MissingKeyException;
import com.google.common.base.Optional;

import java.io.Serializable;
import java.util.Date;

public class DataHubKey implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final long MIN_SEQUENCE = 1000;
    private final long sequence;

    public DataHubKey(long sequence) {
        if (sequence < MIN_SEQUENCE)
        {
            throw new MissingKeyException("sequence number is too small " + sequence);
        }
        this.sequence = sequence;
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

        return true;
    }

    @Override
    public int hashCode() {
        return (int) (sequence ^ (sequence >>> 32));
    }

    @Override
    public String toString() {
        return "DataHubKey{" +
                " sequence=" + sequence +
                '}';
    }
}
