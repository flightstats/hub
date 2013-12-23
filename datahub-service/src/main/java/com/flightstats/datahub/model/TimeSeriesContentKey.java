package com.flightstats.datahub.model;

import com.google.common.base.Optional;

import java.util.UUID;

/**
 *
 */
public class TimeSeriesContentKey implements ContentKey {

    private final String id;

    public TimeSeriesContentKey() {
        id = UUID.randomUUID().toString();
    }

    private TimeSeriesContentKey(String id) {
        this.id = id;
    }

    @Override
    public Optional<ContentKey> getNext() {
        return Optional.absent();
    }

    @Override
    public Optional<ContentKey> getPrevious() {
        return Optional.absent();
    }

    @Override
    public String keyToString() {
        return id;
    }

    public static Optional<ContentKey> fromString(String id) {
        try {
            Optional<? extends ContentKey> optional = Optional.of(new TimeSeriesContentKey(id));
            return (Optional<ContentKey>) optional;
        } catch (Exception e) {
            return Optional.absent();
        }
    }
}
