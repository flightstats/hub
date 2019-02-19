package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

/**
 * A MinutePath represents the end of a minute period.
 * So any ContentKeys contained within that minute come before the MinutePath.
 */
public class MinutePath implements ContentPathKeys {
    private final static Logger logger = LoggerFactory.getLogger(MinutePath.class);

    public static final MinutePath NONE = new MinutePath(new DateTime(1, DateTimeZone.UTC));

    private final DateTime time;
    private final Collection<ContentKey> keys;

    public MinutePath(DateTime time, Collection<ContentKey> keys) {
        this.time = TimeUtil.Unit.MINUTES.round(time);
        this.keys = keys;
    }

    public MinutePath(DateTime time) {
        this(time, Collections.emptyList());
    }

    public MinutePath() {
        this(TimeUtil.now().minusMinutes(1));
    }

    @Override
    public byte[] toBytes() {
        return toUrl().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String toUrl() {
        return TimeUtil.minutes(time);
    }

    @Override
    public String toZk() {
        return "" + time.getMillis();
    }

    @Override
    public MinutePath fromZk(String value) {
        return new MinutePath(new DateTime(Long.parseLong(value), DateTimeZone.UTC));
    }

    @Override
    public int compareTo(ContentPath other) {
        if (other instanceof MinutePath) {
            return time.compareTo(other.getTime());
        } else {
            DateTime endTime = getTime().plusMinutes(1);
            int diff = endTime.compareTo(other.getTime());
            if (diff == 0) {
                return -1;
            }
            return diff;
        }
    }

    public static MinutePath fromBytes(byte[] bytes) {
        return fromUrl(new String(bytes, StandardCharsets.UTF_8)).get();
    }

    public static Optional<MinutePath> fromUrl(String key) {
        try {
            return Optional.of(new MinutePath(TimeUtil.minutes(key)));
        } catch (Exception e) {
            logger.trace("unable to parse {} {}", key, e.getMessage());
            return Optional.empty();
        }
    }

    @Override
    public String toString() {
        return toUrl();
    }

    public MinutePath addMinute() {
        return new MinutePath(time.plusMinutes(1));
    }

    public DateTime getTime() {
        return this.time;
    }

    public Collection<ContentKey> getKeys() {
        return this.keys;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof MinutePath)) return false;
        final MinutePath other = (MinutePath) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$time = this.getTime();
        final Object other$time = other.getTime();
        if (this$time == null ? other$time != null : !this$time.equals(other$time)) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $time = this.getTime();
        result = result * PRIME + ($time == null ? 43 : $time.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof MinutePath;
    }
}
