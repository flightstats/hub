package com.flightstats.rest;

import java.net.URI;

public class HalLink {
    private final String name;
    private final URI uri;

    public HalLink(String name, URI uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return name;
    }

    public URI getUri() {
        return uri;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HalLink halLink = (HalLink) o;

        if (name != null ? !name.equals(halLink.name) : halLink.name != null) {
            return false;
        }
        if (uri != null ? !uri.equals(halLink.uri) : halLink.uri != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (uri != null ? uri.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "HalLink{" +
                "name='" + name + '\'' +
                ", uri=" + uri +
                '}';
    }
}

