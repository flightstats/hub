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

import java.text.DecimalFormat;

@EqualsAndHashCode
@Getter
public class ContentKey implements ContentPath {
    public static final ContentKey NONE = new ContentKey(new DateTime(1, DateTimeZone.UTC), "none");
    private final static Logger logger = LoggerFactory.getLogger(ContentKey.class);
    private final DateTime time;
    private final String hash;

    private static final DecimalFormat format = new DecimalFormat("000000");

    public ContentKey() {
        this(TimeUtil.now());
    }

    public ContentKey(DateTime time) {
        this(time, RandomStringUtils.randomAlphanumeric(6));
    }

    public ContentKey(DateTime time, String hash) {
        this.time = time;
        this.hash = hash;
    }

    public static ContentKey lastKey(DateTime time) {
        return new ContentKey(time, "~ZZZZZZZZZZZZZZZZ");
    }

    public ContentKey(int year, int month, int day, int hour, int minute, int second, int millis, String hash) {
        this(new DateTime(year, month, day, hour, minute, second, millis, DateTimeZone.UTC), hash);
    }

    public static Optional<ContentKey> fromFullUrl(String url) {
        try {
            String substring = StringUtils.substringAfter(url, "/channel/");
            substring = StringUtils.substringAfter(substring, "/");
            return fromUrl(substring);
        } catch (Exception e) {
            logger.info("unable to parse " + url + " " + e.getMessage());
            return Optional.absent();
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
            logger.trace("unable to parse {} {} ", key, e.getMessage());
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
    public int compareTo(ContentPath other) {
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
        return toUrl().getBytes(Charsets.UTF_8);
    }

    public String toZk() {
        return time.getMillis() + ":" + hash;
    }

    public ContentKey fromZk(String value) {
        String[] split = value.split(":");
        return new ContentKey(new DateTime(Long.parseLong(split[0]), DateTimeZone.UTC), split[1]);
    }

    public synchronized static String bulkHash(int number) {
        return format.format(number);
    }

    public static ContentKey bulkKey(ContentKey master, int index) {
        return new ContentKey(master.getTime(), master.getHash() + ContentKey.bulkHash(index));
    }
}
