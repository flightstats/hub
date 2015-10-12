package com.flightstats.hub.spoke;

import com.flightstats.hub.util.TimeUtil;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;

@Builder
@Getter
@EqualsAndHashCode(of = {"dateTime", "hash"})
@ToString
public class Failure implements Comparable<Failure> {
    private String message;
    private Exception exception;
    private final DateTime dateTime = TimeUtil.now();
    private final String hash = RandomStringUtils.randomAlphanumeric(6);

    @Override
    public int compareTo(Failure other) {
        int diff = dateTime.compareTo(other.dateTime);
        if (diff == 0) {
            diff = hash.compareTo(other.hash);
        }
        return diff;
    }
}
