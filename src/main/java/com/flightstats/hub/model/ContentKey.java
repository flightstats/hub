package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.io.Charsets;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode
@Getter
public class ContentKey implements Comparable<ContentKey> {
    public static final ContentKey NONE = new ContentKey(new DateTime(1, DateTimeZone.UTC), "none");
    private final static Logger logger = LoggerFactory.getLogger(ContentKey.class);
    private final DateTime time;
    private final String hash;

    public ContentKey() {
        this(new DateTime(DateTimeZone.UTC), RandomStringUtils.randomAlphanumeric(6));
    }

    public ContentKey(DateTime time, String hash) {
        this.time = time;
        this.hash = hash;
    }

    public static Optional<ContentKey> fromUrl(String key) {
        try {
            String date = StringUtils.substringBeforeLast(key, "/") + "/";
            String hash = StringUtils.substringAfterLast(key, "/");
            return Optional.of(new ContentKey(TimeUtil.millis(date), hash));
        } catch (Exception e) {
            logger.info("unable to parse " + key + " " + e.getMessage());
            return Optional.absent();
        }
    }

    public static ContentKey fromBytes(byte[] bytes) {
        return fromUrl(new String(bytes, Charsets.UTF_8)).get();
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
    public int compareTo(ContentKey other) {
        int diff = time.compareTo(other.time);
        if (diff == 0) {
            diff = hash.compareTo(other.hash);
        }
        return diff;
    }

    public byte[] getBytes() {
        return toUrl().getBytes(Charsets.UTF_8);
    }

    public String toZk() {
        return time.getMillis() + ":" + hash;
    }

    public static ContentKey fromZk(String value) {
        String[] split = value.split(":");
        return new ContentKey(new DateTime(Long.parseLong(split[0]), DateTimeZone.UTC), split[1]);
    }
}
