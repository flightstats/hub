package com.flightstats.datahub.model;

import java.io.Serializable;
import java.util.Date;

public class ChannelConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

	private final String name;
	private final Date creationDate;
	private final Long ttlMillis;

	public ChannelConfiguration(String name, Date creationDate, Long ttlMillis) {
		this.creationDate = creationDate;
		this.name = name;
		this.ttlMillis = ttlMillis;
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

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChannelConfiguration)) return false;

		ChannelConfiguration that = (ChannelConfiguration) o;

		if (creationDate != null ? !creationDate.equals(that.creationDate) : that.creationDate != null) return false;
		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (ttlMillis != null ? !ttlMillis.equals(that.ttlMillis) : that.ttlMillis != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (creationDate != null ? creationDate.hashCode() : 0);
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
}
