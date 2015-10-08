package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.io.Charsets;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

/**
 * A MinutePath represents the end of a minute period.
 * So any ContentKeys contained within that minute come before the MinutePath.
 */
@Getter
@EqualsAndHashCode(of = "time")
public class MinutePath implements ContentPath {
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
        this(TimeUtil.Unit.MINUTES.round(TimeUtil.now()).minusMinutes(1));
    }

    @Override
    public byte[] toBytes() {
        return toUrl().getBytes(Charsets.UTF_8);
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
            ContentKey contentKey = (ContentKey) other;
            DateTime endTime = getTime().plusMinutes(1);
            int diff = endTime.compareTo(contentKey.getTime());
            if (diff == 0) {
                return -1;
            }
            return diff;
        }
    }

    public static MinutePath fromBytes(byte[] bytes) {
        return fromUrl(new String(bytes, Charsets.UTF_8)).get();
    }

    public static Optional<MinutePath> fromUrl(String key) {
        try {
            return Optional.of(new MinutePath(TimeUtil.minutes(key)));
        } catch (Exception e) {
            logger.info("unable to parse " + key + " " + e.getMessage());
            return Optional.absent();
        }
    }

    @Override
    public String toString() {
        return toUrl();
    }
}
