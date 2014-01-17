package com.flightstats.datahub.model;

import com.google.common.base.Optional;

@SuppressWarnings({"unchecked", "ConstantConditions"})
public class SequenceContentKey implements ContentKey {
    private static final long serialVersionUID = 1L;
    private final long sequence;
    public static final long START_VALUE = 1000;

    public SequenceContentKey(long sequence) {
        this.sequence = sequence;
    }

    @Override
    public Optional<ContentKey> getNext() {
        Optional<? extends ContentKey> optional = Optional.of(new SequenceContentKey(sequence + 1));
        return (Optional<ContentKey>) optional;
    }

    @Override
    public Optional<ContentKey> getPrevious() {
        Optional<? extends ContentKey> optional = Optional.of(new SequenceContentKey(sequence - 1));
        return (Optional<ContentKey>) optional;
    }

    public long getSequence() {
        return sequence;
    }

    @Override
    public String keyToString() {
        return Long.toString(getSequence());
    }

    public static Optional<ContentKey> fromString(String key) {
        try {
            Optional<? extends ContentKey> optional = Optional.of(new SequenceContentKey(Long.parseLong(key)));
            return (Optional<ContentKey>) optional;
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

        SequenceContentKey that = (SequenceContentKey) o;

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
        return "SequenceContentKey{" +
                " sequence=" + sequence +
                '}';
    }

    @Override
    public int compareTo(ContentKey o) {
        if (o instanceof SequenceContentKey) {
            SequenceContentKey other = (SequenceContentKey) o;
            return (int) (sequence - other.sequence);
        } else {
            return keyToString().compareTo(o.keyToString());
        }
    }
}
