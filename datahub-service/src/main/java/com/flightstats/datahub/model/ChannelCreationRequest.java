package com.flightstats.datahub.model;

import com.google.common.base.Optional;
import org.codehaus.jackson.map.exc.UnrecognizedPropertyException;

import java.util.Map;
import java.util.concurrent.TimeUnit;

public class ChannelCreationRequest {

	public static final Long DEFAULT_TTL = TimeUnit.DAYS.toMillis(120);

	private final Optional<String> name;
	private final Optional<Long> ttlMillis;

	protected ChannelCreationRequest(Builder builder) {
		name = builder.name;
		ttlMillis = builder.ttlMillis;
	}

	protected static ChannelCreationRequest create(Map<String, String> props) throws UnrecognizedPropertyException {
		Builder builder = builder();
		for (Map.Entry<String, String> entry : props.entrySet()) {
			switch (entry.getKey()) {
				case "name":
					builder.withName(entry.getValue());
					break;
				case "ttlMillis":
					builder.withTtlMillis(entry.getValue() == null ? null : Long.parseLong(entry.getValue()));
					break;
				default:
					throw new UnrecognizedPropertyException("Unexpected property: " + entry.getKey(), null, ChannelCreationRequest.class, entry.getKey());
			}
		}
		return builder.build();
	}

	public Optional<String> getName() {
		return name;
	}

	public Optional<Long> getTtlMillis() {
		return ttlMillis;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChannelCreationRequest)) return false;

		ChannelCreationRequest that = (ChannelCreationRequest) o;

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
		return "ChannelCreationRequest{" +
			"name='" + name + '\'' +
			", ttl=" + ttlMillis +
			'}';
	}

	public static Builder builder() {
		return new Builder();
	}

	public static class Builder {
		private Optional<String> name = null;
		private Optional<Long> ttlMillis = Optional.of(DEFAULT_TTL);

		public Builder withName(String name) {
			this.name = Optional.fromNullable(name);
			return this;
		}

		public Builder withTtlMillis(Long ttlMillis) {
			this.ttlMillis = Optional.fromNullable(ttlMillis);
			return this;
		}

		public ChannelCreationRequest build() {
			return new ChannelCreationRequest(this);
		}
	}
}
