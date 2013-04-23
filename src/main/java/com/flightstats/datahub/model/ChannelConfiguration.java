package com.flightstats.datahub.model;

import java.io.Serializable;
import java.util.Date;

public class ChannelConfiguration implements Serializable {
    private static final long serialVersionUID = 1L;

	private final String name;
	private final Date creationDate;

	public ChannelConfiguration(String name, Date creationDate) {
		this.creationDate = creationDate;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		ChannelConfiguration that = (ChannelConfiguration) o;

		if (!creationDate.equals(that.creationDate)) {
			return false;
		}
		if (!name.equals(that.name)) {
			return false;
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = name.hashCode();
		result = 31 * result + creationDate.hashCode();
		return result;
	}

	@Override
	public String toString() {
		return "ChannelConfiguration{" +
				"name='" + name + '\'' +
				", creationDate=" + creationDate +
				'}';
	}
}
