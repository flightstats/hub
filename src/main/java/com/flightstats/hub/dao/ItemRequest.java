package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;

import java.net.URI;
import java.util.Date;

public class ItemRequest {
    private final String channel;
    private final String tag;
    private final URI uri;
    private final ContentKey key;
    private Date date = new Date();
    private boolean remoteOnly = false;

    @java.beans.ConstructorProperties({"channel", "tag", "uri", "key", "date", "remoteOnly"})
    private ItemRequest(String channel, String tag, URI uri, ContentKey key, Date date, boolean remoteOnly) {
        this.channel = channel;
        this.tag = tag;
        this.uri = uri;
        this.key = key;
        this.date = date;
        this.remoteOnly = remoteOnly;
    }

    public static ItemRequestBuilder builder() {
        return new ItemRequestBuilder();
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

    public boolean isRemoteOnly() {
        return this.remoteOnly;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ItemRequest)) return false;
        final ItemRequest other = (ItemRequest) o;
        if (!other.canEqual(this)) return false;
        final Object this$channel = this.getChannel();
        final Object other$channel = other.getChannel();
        if (this$channel == null ? other$channel != null : !this$channel.equals(other$channel)) return false;
        final Object this$key = this.getKey();
        final Object other$key = other.getKey();
        return this$key == null ? other$key == null : this$key.equals(other$key);
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
        return other instanceof ItemRequest;
    }

    public String toString() {
        return "com.flightstats.hub.dao.ItemRequest(channel=" + this.getChannel() + ", tag=" + this.getTag() + ", uri=" + this.getUri() + ", key=" + this.getKey() + ", date=" + this.getDate() + ", remoteOnly=" + this.isRemoteOnly() + ")";
    }

    public ItemRequest withChannel(String channel) {
        return this.channel == channel ? this : new ItemRequest(channel, this.tag, this.uri, this.key, this.date, this.remoteOnly);
    }

    public static class ItemRequestBuilder {
        private String channel;
        private String tag;
        private URI uri;
        private ContentKey key;
        private Date date;
        private boolean remoteOnly;

        ItemRequestBuilder() {
        }

        public ItemRequest.ItemRequestBuilder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public ItemRequest.ItemRequestBuilder tag(String tag) {
            this.tag = tag;
            return this;
        }

        public ItemRequest.ItemRequestBuilder uri(URI uri) {
            this.uri = uri;
            return this;
        }

        public ItemRequest.ItemRequestBuilder key(ContentKey key) {
            this.key = key;
            return this;
        }

        public ItemRequest.ItemRequestBuilder date(Date date) {
            this.date = date;
            return this;
        }

        public ItemRequest.ItemRequestBuilder remoteOnly(boolean remoteOnly) {
            this.remoteOnly = remoteOnly;
            return this;
        }

        public ItemRequest build() {
            return new ItemRequest(channel, tag, uri, key, date, remoteOnly);
        }

        public String toString() {
            return "com.flightstats.hub.dao.ItemRequest.ItemRequestBuilder(channel=" + this.channel + ", tag=" + this.tag + ", uri=" + this.uri + ", key=" + this.key + ", date=" + this.date + ", remoteOnly=" + this.remoteOnly + ")";
        }
    }
}
