package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContentKeyUtil {

    //todo - gfm - 10/7/15 - could be switched to ContentPath - not sure it's needed
    public static SortedSet<ContentKey> filter(Collection<ContentKey> keys, ContentKey limitKey,
                                         DateTime ttlTime, int count, boolean next, boolean stable) {
        Stream<ContentKey> stream = keys.stream();
        if (next) {
            DateTime stableTime = TimeUtil.time(stable);
            stream = stream
                    .filter(key -> key.compareTo(limitKey) > 0)
                    .filter(key -> key.getTime().isBefore(stableTime));

        } else {
            Collection<ContentKey> contentKeys = new TreeSet<>(Collections.reverseOrder());
            contentKeys.addAll(keys);
            stream = contentKeys.stream()
                    .filter(key -> key.compareTo(limitKey) < 0);
        }
        return stream
                .filter(key -> key.getTime().isAfter(ttlTime))
                .limit(count)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    public static SortedSet<MinutePath> convert(SortedSet<ContentKey> keys) {
        Map<DateTime, MinutePath> minutes = new TreeMap<>();
        for (ContentKey key : keys) {
            DateTime time = TimeUtil.Unit.MINUTES.round(key.getTime());
            MinutePath minutePath = minutes.get(time);
            if (minutePath == null) {
                minutePath = new MinutePath(time, new TreeSet<>());
                minutes.put(time, minutePath);
            }
            minutePath.getKeys().add(key);
        }
        return new TreeSet<>(minutes.values());
    }
}
