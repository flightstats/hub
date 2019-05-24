package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;

import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.Optional;

@Slf4j
public class ContentKey implements ContentPath {
    public static final ContentKey NONE = new ContentKey(TimeUtil.BIG_BANG, "none");
    private static final DecimalFormat format = new DecimalFormat("000000");
    private final DateTime time;
    private final String hash;

    public ContentKey() {
        this(TimeUtil.now());
    }

    public ContentKey(DateTime time) {
        this(time, com.flightstats.hub.util.StringUtils.randomAlphaNumeric(6));
    }

    public ContentKey(DateTime time, String hash) {
        this.time = time;
        this.hash = hash;
    }

    public ContentKey(int year, int month, int day, int hour, int minute, int second, int millis) {
        this(new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC));
    }

    public ContentKey(int year, int month, int day, int hour, int minute, int second, int millis, String hash) {
        this(new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC), hash);
    }

    public static ContentKey lastKey(DateTime time) {
        return new ContentKey(time, "~ZZZZZZZZZZZZZZZZ");
    }

    public static ContentKey fromFullUrl(String url) {
        try {
            String substring = StringUtils.substringAfter(url, "/channel/");
            substring = StringUtils.substringAfter(substring, "/");
            return fromUrl(substring).get();
        } catch (Exception e) {
            log.info("unable to parse " + url + " " + e.getMessage());
            return null;
        }
    }

    public static Optional<ContentKey> fromUrl(String key) {
        try {
            int year = Integer.parseInt(key.substring(0, 4));
            int month = Integer.parseInt(key.substring(5, 7));
            int day = Integer.parseInt(key.substring(8, 10));
            int hour = Integer.parseInt(key.substring(11, 13));
            int minute = Integer.parseInt(key.substring(14, 16));
            int second = Integer.parseInt(key.substring(17, 19));
            int millis = Integer.parseInt(key.substring(20, 23));
            String hash = key.substring(24);
            DateTime dateTime = new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC);
            return Optional.of(new ContentKey(dateTime, hash));
        } catch (Exception e) {
            log.trace("unable to parse {} {} ", key, e.getMessage());
            return Optional.empty();
        }
    }

    private synchronized static String bulkHash(int number) {
        return format.format(number);
    }

    public static ContentKey bulkKey(ContentKey master, int index) {
        return new ContentKey(master.getTime(), master.getHash() + ContentKey.bulkHash(index));
    }

    public String toUrl() {
        return TimeUtil.millis(time) + hash;
    }

    public long getMillis() {
        return time.getMillis();
    }

    public String toString(DateTimeFormatter pathFormatter) {
        return time.toString(pathFormatter) + hash;
    }

    @Override
    public String toString() {
        return toUrl();
    }

    @Override
    public int compareTo(ContentPath other) {
        if (other == null) {
            return 1;
        }
        if (other instanceof ContentKey) {
            ContentKey key = (ContentKey) other;
            int diff = time.compareTo(key.getTime());
            if (diff == 0) {
                diff = hash.compareTo(key.hash);
            }
            return diff;
        }
        if (other instanceof SecondPath) {
            SecondPath secondPath = (SecondPath) other;
            DateTime endTime = secondPath.getTime().plusSeconds(1);
            int diff = time.compareTo(endTime);
            if (diff == 0) {
                return 1;
            }
            return diff;

        } else {
            MinutePath minutePath = (MinutePath) other;
            DateTime endTime = minutePath.getTime().plusMinutes(1);
            int diff = time.compareTo(endTime);
            if (diff == 0) {
                return 1;
            }
            return diff;
        }
    }

    public byte[] toBytes() {
        return toUrl().getBytes(StandardCharsets.UTF_8);
    }

    public String toZk() {
        return time.getMillis() + ":" + hash;
    }

    public ContentKey fromZk(String value) {
        String[] split = value.split(":");
        return new ContentKey(new DateTime(Long.parseLong(split[0]), DateTimeZone.UTC), split[1]);
    }

    public DateTime getTime() {
        return this.time;
    }

    public String getHash() {
        return this.hash;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ContentKey)) return false;
        final ContentKey other = (ContentKey) o;
        if (!other.canEqual(this)) return false;
        final Object this$time = this.getTime();
        final Object other$time = other.getTime();
        if (this$time == null ? other$time != null : !this$time.equals(other$time)) return false;
        final Object this$hash = this.getHash();
        final Object other$hash = other.getHash();
        return this$hash == null ? other$hash == null : this$hash.equals(other$hash);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $time = this.getTime();
        result = result * PRIME + ($time == null ? 43 : $time.hashCode());
        final Object $hash = this.getHash();
        result = result * PRIME + ($hash == null ? 43 : $hash.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof ContentKey;
    }
}
