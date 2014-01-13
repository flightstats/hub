package com.flightstats.datahub.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChannelConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final Long DEFAULT_TTL = TimeUnit.DAYS.toMillis(120);
	private final String name;
	private final Date creationDate;
	private final Long ttlMillis;
    private final ChannelType type;
    private final int contentSizeKB;
    private final int peakRequestRateSeconds;

    public enum ChannelType { Sequence, TimeSeries }

    public ChannelConfiguration(String name, Date creationDate, Long ttlMillis, ChannelType type,
                                int contentSizeKB, int peakRequestRateSeconds) {
        this.name = name;
        this.creationDate = creationDate;
        this.ttlMillis = ttlMillis;
        this.type = type;
        this.contentSizeKB = contentSizeKB;
        this.peakRequestRateSeconds = peakRequestRateSeconds;
    }

    @JsonCreator
    protected static ChannelConfiguration create(Map<String, String> props) throws UnrecognizedPropertyException {
        Builder builder = builder();
        for (Map.Entry<String, String> entry : props.entrySet()) {
            switch (entry.getKey()) {
                case "name":
                    builder.withName(entry.getValue().trim());
                    break;
                case "ttlMillis":
                    builder.withTtlMillis(entry.getValue() == null ? null : Long.parseLong(entry.getValue()));
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
                    throw new UnrecognizedPropertyException("Unexpected property: " + entry.getKey(), null, ChannelConfiguration.class, entry.getKey(), null);
            }
        }
        return builder.build();
    }

    @JsonProperty("name")
    public String getName() {
		return name;
	}

    @JsonProperty("creationDate")
	public Date getCreationDate() {
		return creationDate;
	}

    @JsonProperty("ttlMillis")
	public Long getTtlMillis() {
		return ttlMillis;
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

    @JsonIgnore()
    public ChannelType getType() {
        return type;
    }

    @Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChannelConfiguration)) return false;

		ChannelConfiguration that = (ChannelConfiguration) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (ttlMillis != null ? !ttlMillis.equals(that.ttlMillis) : that.ttlMillis != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (ttlMillis != null ? ttlMillis.hashCode() : 0);
		return result;
	}

    @Override
    public String toString() {
        return "ChannelConfiguration{" +
                "name='" + name + '\'' +
                ", creationDate=" + creationDate +
                ", ttlMillis=" + ttlMillis +
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
		private Long ttlMillis = DEFAULT_TTL;
        private ChannelType type = ChannelType.Sequence;
        private int contentKiloBytes = 10;
        private int peakRequestRateSeconds = 10;

		public Builder withChannelConfiguration(ChannelConfiguration config) {
			this.name = config.name;
			this.creationDate = config.creationDate;
			this.ttlMillis = config.ttlMillis;
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
			this.ttlMillis = ttlMillis;
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

		public ChannelConfiguration build() {
			return new ChannelConfiguration(name, creationDate, ttlMillis, type, contentKiloBytes, peakRequestRateSeconds);
		}
	}
}
