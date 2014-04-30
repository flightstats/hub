package com.flightstats.hub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.flightstats.hub.model.exception.InvalidRequestException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.*;
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
    private final String description;
    private final Set<String> tags = new TreeSet<>();

    private static final Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new HubDateTypeAdapter()).create();

    public enum ChannelType { Sequence, TimeSeries }

    public ChannelConfiguration(Builder builder) {
        this.name = StringUtils.trim(builder.name);
        this.creationDate = builder.creationDate;
        this.type = builder.type;
        this.contentSizeKB = builder.contentSizeKB;
        this.peakRequestRateSeconds = builder.peakRequestRateSeconds;
        if (null == builder.ttlMillis) {
            this.ttlDays = builder.ttlDays;
            this.ttlMillis = TimeUnit.DAYS.toMillis(ttlDays);
        } else {
            int extraDay = 0;
            if (builder.ttlMillis % TimeUnit.DAYS.toMillis(1) > 0) {
                extraDay = 1;
            }
            this.ttlDays = TimeUnit.MILLISECONDS.toDays(builder.ttlMillis) + extraDay;
            this.ttlMillis = builder.ttlMillis;
        }
        if (builder.description == null) {
            this.description = "";
        } else {
            this.description = builder.description;
        }
        this.tags.addAll(builder.tags);
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

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("tags")
    public Set<String> getTags() {
        return tags;
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
                ", ttlMillis=" + ttlMillis +
                ", description='" + description + '\'' +
                ", tags=" + tags +
                '}';
    }

    public static ChannelConfiguration fromJson(String json) {
        if (StringUtils.isEmpty(json)) {
            throw new InvalidRequestException("this method requires at least a json name");
        }
        return gson.fromJson(json, ChannelConfiguration.Builder.class).build();
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
        private String description = "";
        private Set<String> tags = new HashSet<>();

        public Builder() {
        }

        public Builder withChannelConfiguration(ChannelConfiguration config) {
			this.name = config.name;
			this.creationDate = config.creationDate;
			this.ttlDays = config.ttlDays;
            this.type = config.type;
            this.contentSizeKB = config.contentSizeKB;
            this.peakRequestRateSeconds = config.peakRequestRateSeconds;
            this.description = config.description;
            this.tags.addAll(config.getTags());
			return this;
		}

        public Builder withUpdateConfig(ChannelConfiguration config) {
            this.ttlDays = config.ttlDays;
            this.contentSizeKB = config.contentSizeKB;
            this.peakRequestRateSeconds = config.peakRequestRateSeconds;
            this.description = config.description;
            this.tags.addAll(config.getTags());
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

        public Builder withTags(Collection<String> tags) {
            this.tags.addAll(tags);
            return this;
        }
		public ChannelConfiguration build() {
			return new ChannelConfiguration(this);
		}

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }
    }
}
