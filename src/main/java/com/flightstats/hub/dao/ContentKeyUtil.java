package com.flightstats.hub.dao;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContentKeyUtil {

    public static Set<ContentKey> filter(Collection<ContentKey> keys, ContentKey limitKey,
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
}
