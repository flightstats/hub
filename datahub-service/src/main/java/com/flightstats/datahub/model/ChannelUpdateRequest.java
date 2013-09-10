package com.flightstats.datahub.model;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.common.base.Optional;

import java.util.Map;

public class ChannelUpdateRequest {

	private final Optional<Long> ttlMillis;

	protected ChannelUpdateRequest(Builder builder) {
		ttlMillis = builder.ttlMillis;
	}

	protected static ChannelUpdateRequest create(Map<String, String> props) throws UnrecognizedPropertyException {
		Builder builder = builder();
		for (Map.Entry<String, String> entry : props.entrySet()) {
			switch (entry.getKey()) {
				case "ttlMillis":
					builder.withTtlMillis(entry.getValue() == null ? null : Long.parseLong(entry.getValue()));
					break;
				default:
					throw new UnrecognizedPropertyException("Unexpected property", null, ChannelUpdateRequest.class, entry.getKey(), null);
			}
		}
		return builder.build();
	}

	public Optional<Long> getTtlMillis() {
		return ttlMillis;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChannelUpdateRequest)) return false;

		ChannelUpdateRequest that = (ChannelUpdateRequest) o;

		if (ttlMillis != null ? !ttlMillis.equals(that.ttlMillis) : that.ttlMillis != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		return ttlMillis != null ? ttlMillis.hashCode() : 0;
	}

	@Override
	public String toString() {
		return "ChannelUpdateRequest{" +
			"ttlMillis=" + ttlMillis +
			'}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Optional<Long> ttlMillis = null;

		public Builder withTtlMillis(Long ttlMillis) {
			this.ttlMillis = Optional.fromNullable(ttlMillis);
			return this;
		}

		public ChannelUpdateRequest build() {
			return new ChannelUpdateRequest(this);
		}
	}
}
