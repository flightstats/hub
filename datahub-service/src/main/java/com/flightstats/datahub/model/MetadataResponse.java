package com.flightstats.datahub.model;

import java.util.Date;

public class MetadataResponse {

	private final ChannelConfiguration config;

	public MetadataResponse(ChannelConfiguration config) {
		this.config = config;
	}

	public String getName() {
		return config.getName();
	}

	public Date getCreationDate() {
		return config.getCreationDate();
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

		return true;
	}

	@Override
	public int hashCode() {
        return config.hashCode();
	}

	@Override
	public String toString() {
		return "MetadataResponse{" +
				"config=" + config +
				'}';
	}
}
