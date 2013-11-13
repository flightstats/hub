package com.flightstats.datahub.model;

import com.flightstats.datahub.dao.SequenceRowKeyStrategy;
import com.google.common.base.Optional;

import java.io.Serializable;

public class DataHubKey implements Serializable {
    private static final long serialVersionUID = 1L;
    private final long sequence;

    public DataHubKey(long sequence) {
        this.sequence = sequence;
    }

    public Optional<DataHubKey> getNext() {
        return Optional.of(new DataHubKey(sequence + 1));
    }

    public Optional<DataHubKey> getPrevious() {
        return Optional.of(new DataHubKey(sequence - 1));
    }

    public long getSequence() {
        return sequence;
    }

    public boolean isNewRow()
    {
        return sequence % SequenceRowKeyStrategy.INCREMENT == 0;
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
