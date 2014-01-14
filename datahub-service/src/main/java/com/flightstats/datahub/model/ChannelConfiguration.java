package com.flightstats.datahub.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightstats.datahub.model.exception.InvalidRequestException;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChannelConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
	private final Date creationDate;
	private final long ttlDays;
    private final ChannelType type;
    private final int contentSizeKB;
    private final int peakRequestRateSeconds;

    public enum ChannelType { Sequence, TimeSeries }

    public ChannelConfiguration(String name, Date creationDate, Long ttlDays, ChannelType type,
                                int contentSizeKB, int peakRequestRateSeconds) {
        this.name = name;
        this.creationDate = creationDate;
        this.ttlDays = ttlDays;
        this.type = type;
        this.contentSizeKB = contentSizeKB;
        this.peakRequestRateSeconds = peakRequestRateSeconds;
    }

    @JsonCreator
    protected static ChannelConfiguration create(Map<String, String> props) {
        Builder builder = builder();
        populate(props, builder);
        return builder.build();
    }

    public static void populate(Map<String, String> props, Builder builder) {
        for (Map.Entry<String, String> entry : props.entrySet()) {
            switch (entry.getKey()) {
                case "name":
                    builder.withName(entry.getValue().trim());
                    break;
                case "ttlMillis":
                    builder.withTtlMillis(entry.getValue() == null ? null : Long.parseLong(entry.getValue()));
                    break;
                case "ttlDays":
                    builder.withTtlDays(Long.parseLong(entry.getValue()));
                    break;
                case "type":
                    builder.withType(ChannelType.valueOf(entry.getValue()));
                    break;
                case "contentSizeKB":
                    builder.withContentKiloBytes(Integer.parseInt(entry.getValue()));
                    break;
                case "peakRequestRateSeconds":
                    builder.withPeakRequestRate(Integer.parseInt(entry.getValue()));
                    break;
                default:
                    throw new InvalidRequestException("Unexpected property: " + entry.getKey());
            }
        }
    }

    @JsonProperty("name")
    public String getName() {
		return name;
	}

    @JsonProperty("creationDate")
	public Date getCreationDate() {
		return creationDate;
	}

    @JsonProperty("ttlDays")
	public long getTtlDays() {
		return ttlDays;
	}

    @JsonIgnore()
    public boolean isSequence() {
        return ChannelType.Sequence.equals(type);
    }

    @JsonIgnore()
    public long getContentThroughputInSeconds() {
        return contentSizeKB * getPeakRequestRateSeconds();
    }

    @JsonProperty("contentSizeKB")
    public int getContentSizeKB() {
        return contentSizeKB;
    }

    @JsonProperty("peakRequestRateSeconds")
    public int getPeakRequestRateSeconds() {
        return peakRequestRateSeconds;
    }

    @JsonProperty("type")
    public ChannelType getType() {
        return type;
    }

    @Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChannelConfiguration)) return false;

		ChannelConfiguration that = (ChannelConfiguration) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
        return name != null ? name.hashCode() : 0;
	}

    @Override
    public String toString() {
        return "ChannelConfiguration{" +
                "name='" + name + '\'' +
                ", creationDate=" + creationDate +
                ", ttlDays=" + ttlDays +
                ", type=" + type +
                ", contentSizeKB=" + contentSizeKB +
                ", peakRequestRateSeconds=" + peakRequestRateSeconds +
                '}';
    }

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private Date creationDate = new Date();
		private long ttlDays = 120;
        private ChannelType type = ChannelType.Sequence;
        private int contentKiloBytes = 10;
        private int peakRequestRateSeconds = 10;

		public Builder withChannelConfiguration(ChannelConfiguration config) {
			this.name = config.name;
			this.creationDate = config.creationDate;
			this.ttlDays = config.ttlDays;
            this.type = config.type;
            this.contentKiloBytes = config.contentSizeKB;
            this.peakRequestRateSeconds = config.peakRequestRateSeconds;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

        public Builder withTtlMillis(Long ttlMillis) {
            if (null == ttlMillis) {
                this.ttlDays = 1000 * 365;
            } else {
                this.ttlDays = TimeUnit.MILLISECONDS.toDays(ttlMillis) + 1;
            }
            return this;
        }

		public Builder withTtlDays(long ttlDays) {
			this.ttlDays = ttlDays;
			return this;
		}

		public Builder withCreationDate(Date date) {
			this.creationDate = date;
			return this;
		}

        public Builder withType(ChannelType channelType) {
            this.type = channelType;
            return this;
        }

        public Builder withContentKiloBytes(int contentKiloBytes) {
            this.contentKiloBytes = contentKiloBytes;
            return this;
        }

        public Builder withPeakRequestRate(int peakRequestRateSeconds) {
            this.peakRequestRateSeconds = peakRequestRateSeconds;
            return this;
        }

        public Builder withMap(Map<String, String> valueMap) {
            populate(valueMap, this);
            return this;
        }

		public ChannelConfiguration build() {
			return new ChannelConfiguration(name, creationDate, ttlDays, type, contentKiloBytes, peakRequestRateSeconds);
		}
	}
}
