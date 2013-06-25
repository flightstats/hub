package com.flightstats.datahub.model;

import java.util.Date;

public class MetadataResponse {

	private final ChannelConfiguration config;
	private final Date lastUpdateDate;

	public MetadataResponse(ChannelConfiguration config, Date lastUpdateDate) {
		this.config = config;
		this.lastUpdateDate = lastUpdateDate;
	}

	public String getName() {
		return config.getName();
	}

	public Date getCreationDate() {
		return config.getCreationDate();
	}

	public Date getLastUpdateDate() {
		return lastUpdateDate;
	}

	public Long getTtlMillis() {
		return config.getTtlMillis();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		MetadataResponse that = (MetadataResponse) o;

		if (!config.equals(that.config)) {
			return false;
		}
		if (lastUpdateDate != null ? !lastUpdateDate.equals(that.lastUpdateDate) : that.lastUpdateDate != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = config.hashCode();
		result = 31 * result + (lastUpdateDate != null ? lastUpdateDate.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "MetadataResponse{" +
				"config=" + config +
				", lastUpdateDate=" + lastUpdateDate +
				'}';
	}
}
