package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;
import org.joda.time.DateTime;

import java.util.Optional;
import java.util.stream.Stream;

@Builder
@Value
public class ChannelConfigExpirationSettings {
    String channelName;

    boolean keepForever;
    long ttlDays;
    DateTime mutableTime;
    long maxItems;

    public Optional<DateTime> getMutableTime() {
        return Optional.ofNullable(mutableTime);
    }

    public static class ChannelConfigExpirationSettingsBuilder {
        private boolean keepForever;
        private long ttlDays;
        private DateTime mutableTime;
        private long maxItems;

        public ChannelConfigExpirationSettingsBuilder keepForever(boolean keepForever) {
            this.keepForever = keepForever;
            validate();
            return this;
        }

        public ChannelConfigExpirationSettingsBuilder ttlDays(long ttlDays) {
            this.ttlDays = ttlDays;
            validate();
            return this;
        }

        public ChannelConfigExpirationSettingsBuilder mutableTime(DateTime mutableTime) {
            this.mutableTime = mutableTime;
            validate();
            return this;
        }

        public ChannelConfigExpirationSettingsBuilder maxItems(long maxItems) {
            this.maxItems = maxItems;
            validate();
            return this;
        }

        private void validate() {
            if (Stream.of(keepForever, hasTtlDays(), isHistoricalChannel(), hasMaxItems())
                    .filter(bool -> bool).count() > 1) {
                throw new IllegalStateException("Only one channel expiration type can be set");
            }
        }

        private boolean hasTtlDays() {
            return ttlDays > 0;
        }

        private boolean isHistoricalChannel() {
            return Optional.ofNullable(this.mutableTime).isPresent();
        }

        private boolean hasMaxItems() {
            return maxItems > 0;
        }
    }
}
