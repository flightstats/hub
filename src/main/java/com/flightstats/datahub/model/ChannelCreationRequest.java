package com.flightstats.datahub.model;

public class ChannelCreationRequest {

    private final String name;

    public ChannelCreationRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ChannelCreationRequest that = (ChannelCreationRequest) o;

        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "ChannelCreationRequest{" +
                "name='" + name + '\'' +
                '}';
    }
}
