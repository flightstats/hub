package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EqualsAndHashCode
public class ContentKey {
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

    public static Optional<ContentKey> fromStorage(String key) {
        try {
            String[] split = key.split("-");
            long epochMilli = Long.parseLong(split[0]);
            return Optional.of(new ContentKey(new DateTime(epochMilli, DateTimeZone.UTC), split[1]));
        } catch (Exception e) {
            logger.info("unable to parse " + key + " " + e.getMessage());
            return Optional.absent();
        }
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

    public String toUrl() {
        return TimeUtil.millis(time) + hash;
    }

    public String toStorage() {
        return getMillis() + "-" + hash;
    }

    public long getMillis() {
        return time.getMillis();
    }

    @Override
    public String toString() {
        return toStorage();
    }
}
