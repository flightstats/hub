package com.flightstats.datahub.model;

import java.io.Serializable;
import java.util.Date;

public class ChannelConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

	private final String name;
	private final Date creationDate;
	private final Long ttlMillis;
    private final ChannelType type;

    public enum ChannelType { Sequence, TimeSeries }

    private ChannelConfiguration(String name, Date creationDate, Long ttlMillis, ChannelType type) {
		this.creationDate = creationDate;
		this.name = name;
		this.ttlMillis = ttlMillis;
        this.type = type;
    }

	public String getName() {
		return name;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public Long getTtlMillis() {
		return ttlMillis;
	}

    public boolean isSequence() {
        return ChannelType.Sequence.equals(type);
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
				", ttl=" + ttlMillis +
				'}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private String name;
		private Date creationDate = new Date();
		private Long ttlMillis;
        private ChannelType type = ChannelType.Sequence;

		public Builder withChannelConfiguration(ChannelConfiguration config) {
			this.name = config.name;
			this.creationDate = config.creationDate;
			this.ttlMillis = config.ttlMillis;
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

		public ChannelConfiguration build() {
			return new ChannelConfiguration(name, creationDate, ttlMillis, type);
		}
	}
}
