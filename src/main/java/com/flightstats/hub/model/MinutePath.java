package com.flightstats.hub.model;

import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.io.Charsets;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;

@Getter
@EqualsAndHashCode(of = "time")
@ToString
public class MinutePath implements ContentPath {
    private final static Logger logger = LoggerFactory.getLogger(MinutePath.class);

    public static final MinutePath NONE = new MinutePath(new DateTime(1, DateTimeZone.UTC));

    private final DateTime time;
    private final Collection<ContentKey> keys;

    public MinutePath(DateTime time, Collection<ContentKey> keys) {
        this.time = time;
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
    public ContentPath toContentPath(byte[] bytes) {
        return fromBytes(bytes);
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
    public int compareTo(ContentPath o) {
        return 0;
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

}
