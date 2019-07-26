package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.stream.Stream;

@Value
@Builder(builderClassName = "ChannelConfigExpirationSettingsBuilder")
public class ChannelConfigExpirationSettings {
    String channelName;

    boolean keepForever;
    long ttlDays;
    DateTime mutableTime;
    long maxItems;

    public Optional<DateTime> getMutableTime() {
        return Optional.ofNullable(mutableTime);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder extends ChannelConfigExpirationSettingsBuilder {
        @Override
        public ChannelConfigExpirationSettings build() {
            ChannelConfigExpirationSettings settings = super.build();
            settings.validate();
            return settings;
        }
    }

    private void validate() {
        if (Stream.of(isKeepForever(), hasTtlDays(), isHistoricalChannel(), hasMaxItems())
                .filter(bool -> bool)
                .count() > 1) {
            throw new IllegalStateException("Only one channel expiration type can be set");
        }
    }

    private boolean hasTtlDays() {
        return getTtlDays() > 0;
    }

    private boolean isHistoricalChannel() {
        return getMutableTime().isPresent();
    }

    private boolean hasMaxItems() {
        return getMaxItems() > 0;
    }
}
