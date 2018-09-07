package com.flightstats.hub.model;

import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ChannelContentKey implements Comparable<ChannelContentKey> {

    private final static Logger logger = LoggerFactory.getLogger(ChannelContentKey.class);
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

    /**
     * @param url Expects the format of "http://hub/channel/channelName/yyyy/mm/dd/hh/mm/ss/sss/hash"
     */
    public static ChannelContentKey fromUrl(String url) {
        String channelPath = StringUtils.substringAfter(url, "channel/");
        return fromChannelPath(channelPath);
    }

    /**
     * @param path Expects the format of "/spoke/.+/channelName/yyyy/mm/dd/hh/mm/[ss][sss][hash]"
     */
    public static ChannelContentKey fromSpokePath(String path) {
        String[] split = path.split("/");
        String channel = split[3];
        String year = split[4];
        String month = split[5];
        String day = split[6];
        String hour = split[7];
        String minute = split[8];
        String second = StringUtils.substring(split[9], 0, 2);
        String millisecond = StringUtils.substring(split[9], 2, 5);
        String hash = StringUtils.substring(split[9], 5);
        String channelPath = Stream.of(channel, year, month, day, hour, minute, second, millisecond, hash).collect(Collectors.joining("/"));
        return fromChannelPath(channelPath);
    }

    /**
     * @param path Expects the format of "channelName/yyyy/mm/dd/hh/mm/ss/sss/hash"
     */
    public static ChannelContentKey fromChannelPath(String path) {
        String[] split = StringUtils.split(path, "/", 2);
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

    public Long getAgeMS() {
        DateTime then = this.getContentKey().getTime();
        DateTime now = DateTime.now(DateTimeZone.UTC);
        try {
            if (then.isBefore(now)) {
                Interval delta = new Interval(then, now);
                return delta.toDurationMillis();
            } else {
                Interval delta = new Interval(now, then);
                return -delta.toDurationMillis();
            }
        } catch (IllegalArgumentException e) {
            logger.warn("unable to calculate the item's age", e);
            return null;
        }

    }
}
