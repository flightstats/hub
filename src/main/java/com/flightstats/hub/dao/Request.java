package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;

import java.net.URI;
import java.util.Date;

public class Request {
    private final String channel;
    private final String tag;
    private final URI uri;
    private final ContentKey key;
    private Date date = new Date();

    @java.beans.ConstructorProperties({"channel", "tag", "uri", "key", "date"})
    private Request(String channel, String tag, URI uri, ContentKey key, Date date) {
        this.channel = channel;
        this.tag = tag;
        this.uri = uri;
        this.key = key;
        this.date = date;
    }

    public static RequestBuilder builder() {
        return new RequestBuilder();
    }

    public String getChannel() {
        return this.channel;
    }

    public String getTag() {
        return this.tag;
    }

    public URI getUri() {
        return this.uri;
    }

    public ContentKey getKey() {
        return this.key;
    }

    public Date getDate() {
        return this.date;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Request)) return false;
        final Request other = (Request) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$channel = this.getChannel();
        final Object other$channel = other.getChannel();
        if (this$channel == null ? other$channel != null : !this$channel.equals(other$channel)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        if (this$key == null ? other$key != null : !this$key.equals(other$key)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $channel = this.getChannel();
        result = result * PRIME + ($channel == null ? 43 : $channel.hashCode());
        final Object $key = this.getKey();
        result = result * PRIME + ($key == null ? 43 : $key.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Request;
    }

    public String toString() {
        return "com.flightstats.hub.dao.Request(channel=" + this.getChannel() + ", tag=" + this.getTag() + ", uri=" + this.getUri() + ", key=" + this.getKey() + ", date=" + this.getDate() + ")";
    }

    public Request withChannel(String channel) {
        return this.channel == channel ? this : new Request(channel, this.tag, this.uri, this.key, this.date);
    }

    public static class RequestBuilder {
        private String channel;
        private String tag;
        private URI uri;
        private ContentKey key;
        private Date date;

        RequestBuilder() {
        }

        public Request.RequestBuilder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public Request.RequestBuilder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public Request.RequestBuilder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public Request.RequestBuilder key(ContentKey key) {
            this.key = key;
            return this;
        }

        public Request.RequestBuilder date(Date date) {
            this.date = date;
            return this;
        }

        public Request build() {
            return new Request(channel, tag, uri, key, date);
        }

        public String toString() {
            return "com.flightstats.hub.dao.Request.RequestBuilder(channel=" + this.channel + ", tag=" + this.tag + ", uri=" + this.uri + ", key=" + this.key + ", date=" + this.date + ")";
        }
    }
}
