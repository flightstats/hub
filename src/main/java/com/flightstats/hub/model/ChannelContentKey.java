package com.flightstats.hub.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@EqualsAndHashCode
@Getter
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
}
