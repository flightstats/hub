package com.flightstats.datahub.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.base.Optional;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChannelUpdateRequest {

	private final Optional<Long> ttlMillis;
    private final Optional<Integer> contentKiloBytes;
    private final Optional<Integer> peakRequestRateSeconds;
    private final Optional<TimeUnit> rateTimeUnit;

	protected ChannelUpdateRequest(Builder builder) {
		ttlMillis = builder.ttlMillis;
        contentKiloBytes = builder.contentKiloBytes;
        peakRequestRateSeconds = builder.peakRequestRateSeconds;
        rateTimeUnit = builder.rateTimeUnit;
	}

    @JsonCreator
	protected static ChannelUpdateRequest create(Map<String, String> props) throws UnrecognizedPropertyException {
		Builder builder = builder();
		for (Map.Entry<String, String> entry : props.entrySet()) {
			switch (entry.getKey()) {
				case "ttlMillis":
					builder.withTtlMillis(entry.getValue() == null ? null : Long.parseLong(entry.getValue()));
					break;
                case "contentSizeKB":
                    builder.withContentKiloBytes(Integer.parseInt(entry.getValue()));
                    break;
                case "peakRequestRateSeconds":
                    builder.withPeakRequestRate(Integer.parseInt(entry.getValue()));
                    break;
                case "rateTimeUnit":
                    builder.withRateTimeUnit(TimeUnit.valueOf(entry.getValue().toUpperCase()));
                    break;
				default:
					throw new UnrecognizedPropertyException("Unexpected property", null, ChannelUpdateRequest.class, entry.getKey(), null);
			}
		}
		return builder.build();
	}

	public Optional<Long> getTtlMillis() {
		return ttlMillis;
	}

    public Optional<Integer> getContentKiloBytes() {
        return contentKiloBytes;
    }

    public Optional<Integer> getPeakRequestRate() {
        return peakRequestRateSeconds;
    }

    public Optional<TimeUnit> getRateTimeUnit() {
        return rateTimeUnit;
    }

    @Override
    public String toString() {
        return "ChannelUpdateRequest{" +
                "ttlMillis=" + ttlMillis +
                ", contentKiloBytes=" + contentKiloBytes +
                ", peakRequestRateSeconds=" + peakRequestRateSeconds +
                ", rateTimeUnit=" + rateTimeUnit +
                '}';
    }

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Optional<Long> ttlMillis = null;
        private Optional<Integer> contentKiloBytes = Optional.absent();
        private Optional<Integer> peakRequestRateSeconds = Optional.absent();
        private Optional<TimeUnit> rateTimeUnit = Optional.absent();

		public Builder withTtlMillis(Long ttlMillis) {
			this.ttlMillis = Optional.fromNullable(ttlMillis);
			return this;
		}

        public Builder withContentKiloBytes(int contentKiloBytes) {
            this.contentKiloBytes = Optional.of(contentKiloBytes);
            return this;
        }

        public Builder withPeakRequestRate(int peakRequestRateSeconds) {
            this.peakRequestRateSeconds = Optional.of(peakRequestRateSeconds);
            return this;
        }

        public Builder withRateTimeUnit(TimeUnit rateTimeUnit) {
            this.rateTimeUnit = Optional.of(rateTimeUnit);
            return this;
        }

		public ChannelUpdateRequest build() {
			return new ChannelUpdateRequest(this);
		}
	}
}
