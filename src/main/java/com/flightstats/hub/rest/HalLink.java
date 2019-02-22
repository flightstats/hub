package com.flightstats.hub.rest;

import java.net.URI;

public class HalLink {
    private final String name;
    private final URI uri;

    @java.beans.ConstructorProperties({"name", "uri"})
    public HalLink(String name, URI uri) {
        this.name = name;
        this.uri = uri;
    }

    public String getName() {
        return this.name;
    }

    public URI getUri() {
        return this.uri;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof HalLink)) return false;
        final HalLink other = (HalLink) o;
        if (!other.canEqual(this)) return false;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$uri = this.getUri();
        final Object other$uri = other.getUri();
        return this$uri == null ? other$uri == null : this$uri.equals(other$uri);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $uri = this.getUri();
        result = result * PRIME + ($uri == null ? 43 : $uri.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof HalLink;
    }

    public String toString() {
        return "com.flightstats.hub.rest.HalLink(name=" + this.getName() + ", uri=" + this.getUri() + ")";
    }
}

