package com.flightstats.hub.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightstats.hub.model.exception.InvalidRequestException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
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
    private final Long ttlMillis;
    private static final ObjectMapper mapper = new ObjectMapper();

    public enum ChannelType { Sequence, TimeSeries }

    public ChannelConfiguration(Builder builder) {
        this.name = builder.name;
        this.creationDate = builder.creationDate;
        this.type = builder.type;
        this.contentSizeKB = builder.contentSizeKB;
        this.peakRequestRateSeconds = builder.peakRequestRateSeconds;
        this.ttlDays = builder.ttlDays;
        if (builder.ttlMillis == null) {
            this.ttlMillis = TimeUnit.DAYS.toMillis(ttlDays);
        } else {
            this.ttlMillis = builder.ttlMillis;
        }
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
                case "_links":
                case "lastUpdateDate":
                case "creationDate":
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

    /**
     * @deprecated this can go away eventually, use ttlDays instead
     */
    @JsonProperty("ttlMillis")
    public Long getTtlMillis() {
        return ttlMillis;
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
        private int contentSizeKB = 1;
        private int peakRequestRateSeconds = 1;
        private Long ttlMillis;

        public Builder withChannelConfiguration(ChannelConfiguration config) {
			this.name = config.name;
			this.creationDate = config.creationDate;
			this.ttlDays = config.ttlDays;
            this.type = config.type;
            this.contentSizeKB = config.contentSizeKB;
            this.peakRequestRateSeconds = config.peakRequestRateSeconds;
			return this;
		}

		public Builder withName(String name) {
			this.name = name;
			return this;
		}

        public Builder withTtlMillis(Long ttlMillis) {
            this.ttlMillis = ttlMillis;
            if (null == ttlMillis) {
                this.ttlDays = 1000 * 365;
            } else {
                this.ttlDays = TimeUnit.MILLISECONDS.toDays(ttlMillis);
                if (ttlMillis % TimeUnit.DAYS.toMillis(1) > 0) {
                    this.ttlDays += 1;
                }
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
            this.contentSizeKB = contentKiloBytes;
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

        public Builder withJson(String json) throws IOException {
            Map<String, String> map = new HashMap<>();
            ObjectNode nodes = (ObjectNode) mapper.readTree(json);
            Iterator<Map.Entry<String,JsonNode>> elements = nodes.getFields();
            while (elements.hasNext()) {
                Map.Entry<String, JsonNode> node = elements.next();
                map.put(node.getKey(), node.getValue().asText());
            }
            return withMap(map);
        }

		public ChannelConfiguration build() {
			return new ChannelConfiguration(this);
		}
	}
}
