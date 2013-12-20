package com.flightstats.datahub.model;

import com.google.common.base.Optional;

@SuppressWarnings({"unchecked", "ConstantConditions"})
public class SequenceDataHubKey implements DataHubKey {
    private static final long serialVersionUID = 1L;
    private final long sequence;

    public SequenceDataHubKey(long sequence) {
        this.sequence = sequence;
    }

    @Override
    public Optional<DataHubKey> getNext() {
        Optional<? extends DataHubKey> optional = Optional.of(new SequenceDataHubKey(sequence + 1));
        return (Optional<DataHubKey>) optional;
    }

    @Override
    public Optional<DataHubKey> getPrevious() {
        Optional<? extends DataHubKey> optional = Optional.of(new SequenceDataHubKey(sequence - 1));
        return (Optional<DataHubKey>) optional;
    }

    @Override
    public long getSequence() {
        return sequence;
    }

    @Override
    public String keyToString() {
        return Long.toString(getSequence());
    }

    public static Optional<DataHubKey> fromString(String key) {
        try {
            Optional<? extends DataHubKey> optional = Optional.of(new SequenceDataHubKey(Long.parseLong(key)));
            return (Optional<DataHubKey>) optional;
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SequenceDataHubKey that = (SequenceDataHubKey) o;

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
