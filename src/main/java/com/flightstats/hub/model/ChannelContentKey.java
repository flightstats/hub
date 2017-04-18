package com.flightstats.hub.model;

import org.apache.commons.lang3.StringUtils;

public class ChannelContentKey implements Comparable<ChannelContentKey> {

    private final ContentKey contentKey;
    private final String channel;

    public ChannelContentKey(String channel, ContentKey contentKey) {
        this.channel = channel;
        this.contentKey = contentKey;
    }

    @Override
    public int compareTo(ChannelContentKey o) {
        int diff = contentKey.compareTo(o.getContentKey());
        if (diff == 0) {
            diff = channel.compareTo(o.getChannel());
        }
        return diff;
    }

    public String toUrl() {
        return "channel/" + channel + "/" + contentKey.toUrl();
    }

    public static ChannelContentKey fromUrl(String url) {
        String after = StringUtils.substringAfter(url, "channel/");
        String[] split = StringUtils.split(after, "/", 2);
        return new ChannelContentKey(split[0], ContentKey.fromUrl(split[1]).get());
    }

    @Override
    public String toString() {
        return toUrl();
    }

    public ContentKey getContentKey() {
        return this.contentKey;
    }

    public String getChannel() {
        return this.channel;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ChannelContentKey)) return false;
        final ChannelContentKey other = (ChannelContentKey) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$contentKey = this.getContentKey();
        final Object other$contentKey = other.getContentKey();
        if (this$contentKey == null ? other$contentKey != null : !this$contentKey.equals(other$contentKey))
            return false;
        final Object this$channel = this.getChannel();
        final Object other$channel = other.getChannel();
        if (this$channel == null ? other$channel != null : !this$channel.equals(other$channel)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $contentKey = this.getContentKey();
        result = result * PRIME + ($contentKey == null ? 43 : $contentKey.hashCode());
        final Object $channel = this.getChannel();
        result = result * PRIME + ($channel == null ? 43 : $channel.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ChannelContentKey;
    }
}
