package com.flightstats.datahub.model;

public class ChannelCreationRequest {

    private final String name;
	private final Long ttl;

    public ChannelCreationRequest(String name, Long ttl) {
        this.name = name;
	    this.ttl = ttl;
    }

    public String getName() {
        return name;
    }

	public Long getTtl() {
		return ttl;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ChannelCreationRequest)) return false;

		ChannelCreationRequest that = (ChannelCreationRequest) o;

		if (name != null ? !name.equals(that.name) : that.name != null) return false;
		if (ttl != null ? !ttl.equals(that.ttl) : that.ttl != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = name != null ? name.hashCode() : 0;
		result = 31 * result + (ttl != null ? ttl.hashCode() : 0);
		return result;
	}

	@Override
	public String toString() {
		return "ChannelCreationRequest{" +
				"name='" + name + '\'' +
				", ttl=" + ttl +
				'}';
	}
}
