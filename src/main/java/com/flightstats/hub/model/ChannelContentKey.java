package com.flightstats.hub.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

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

    @Override
    public String toString() {
        return toUrl();
    }
}
