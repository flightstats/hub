package com.flightstats.hub.model;

import com.google.common.base.Optional;

import java.io.Serializable;

@SuppressWarnings({"unchecked", "ConstantConditions"})
public class ContentKey implements Serializable, Comparable<ContentKey> {
    private static final long serialVersionUID = 1L;
    private final long sequence;
    public static final long START_VALUE = 999;

    public ContentKey(long sequence) {
        this.sequence = sequence;
    }

    public ContentKey getNext() {
        return new ContentKey(sequence + 1);
    }

    public ContentKey getPrevious() {
        return new ContentKey(sequence - 1);
    }

    public long getSequence() {
        return sequence;
    }

    public String keyToString() {
        return Long.toString(getSequence());
    }

    public static Optional<ContentKey> fromString(String key) {
        try {
            long keySequence = Long.parseLong(key);
            if (keySequence <= START_VALUE) {
                return Optional.absent();
            }
            Optional<? extends ContentKey> optional = Optional.of(new ContentKey(keySequence));
            return (Optional<ContentKey>) optional;
        } catch (Exception e) {
            return Optional.absent();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ContentKey that = (ContentKey) o;

        if (sequence != that.sequence) return false;

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
        if (o instanceof ContentKey) {
            return (int) (sequence - o.sequence);
        } else {
            return keyToString().compareTo(o.keyToString());
        }
    }
}
