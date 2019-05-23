package com.flightstats.hub.model;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public class ChannelContentKey implements Comparable<ChannelContentKey> {
    private final ContentKey contentKey;
    private final String channel;

    public ChannelContentKey(String channel, ContentKey contentKey) {
        this.channel = channel;
        this.contentKey = contentKey;
    }

    /**
     * @param url Expects the format of ".+/channel/channelName/yyyy/mm/dd/hh/mm/ss/sss/hash"
     */
    public static ChannelContentKey fromResourcePath(String url) {
        String channelPath = StringUtils.substringAfter(url, "channel/");
        return fromChannelPath(channelPath);
    }

    /**
     * @param path Expects the format of ".+/channelName/yyyy/mm/dd/hh/mm/[ss][sss][hash]"
     */
    public static ChannelContentKey fromSpokePath(String path) {
        try {
            String[] split = path.split("/");
            String channel = split[split.length - 7];
            String year = split[split.length - 6];
            String month = split[split.length - 5];
            String day = split[split.length - 4];
            String hour = split[split.length - 3];
            String minute = split[split.length - 2];
            String second = StringUtils.substring(split[split.length - 1], 0, 2);
            String millisecond = StringUtils.substring(split[split.length - 1], 2, 5);
            String hash = StringUtils.substring(split[split.length - 1], 5);
            String channelPath = Stream.of(channel, year, month, day, hour, minute, second, millisecond, hash).map(String::valueOf).collect(Collectors.joining("/"));
            return fromChannelPath(channelPath);
        } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException e) {
            log.error("cannot build key from spoke path: " + path);
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * @param path Expects the format of "channelName/yyyy/mm/dd/hh/mm/ss/sss/hash"
     */
    public static ChannelContentKey fromChannelPath(String path) {
        String[] split = StringUtils.split(path, "/", 2);
        String channelName = split[0];
        Optional<ContentKey> key = ContentKey.fromUrl(split[1]);
        if (key.isPresent()) {
            return new ChannelContentKey(channelName, key.get());
        } else {
            throw new IllegalArgumentException("cannot build key from path: " + path);
        }
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
        if (!other.canEqual(this)) return false;
        final Object this$contentKey = this.getContentKey();
        final Object other$contentKey = other.getContentKey();
        if (this$contentKey == null ? other$contentKey != null : !this$contentKey.equals(other$contentKey))
            return false;
        final Object this$channel = this.getChannel();
        final Object other$channel = other.getChannel();
        return this$channel == null ? other$channel == null : this$channel.equals(other$channel);
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

    public long getAgeMS() {
        DateTime then = this.getContentKey().getTime();
        DateTime now = DateTime.now(DateTimeZone.UTC);
        if (then.isBefore(now)) {
            Interval delta = new Interval(then, now);
            return delta.toDurationMillis();
        } else {
            Interval delta = new Interval(now, then);
            return -delta.toDurationMillis();
        }
    }
}
