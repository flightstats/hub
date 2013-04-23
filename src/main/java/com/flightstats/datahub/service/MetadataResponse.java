package com.flightstats.datahub.service;

import com.flightstats.datahub.model.ChannelConfiguration;

import java.util.Date;

public class MetadataResponse {

	private final ChannelConfiguration config;
	private final Date latestUpdateDate;

	public MetadataResponse(ChannelConfiguration config, Date latestUpdateDate) {
		this.config = config;
		this.latestUpdateDate = latestUpdateDate;
	}

	public String getName() {
		return config.getName();
	}

	public Date getCreationDate() {
		return config.getCreationDate();
	}

	public Date getLastUpdateDate() {
		return latestUpdateDate;
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
		if (latestUpdateDate != null ? !latestUpdateDate.equals(that.latestUpdateDate) : that.latestUpdateDate != null) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = config.hashCode();
		result = 31 * result + (latestUpdateDate != null ? latestUpdateDate.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "MetadataResponse{" +
				"config=" + config +
				", latestUpdateDate=" + latestUpdateDate +
				'}';
	}
}
