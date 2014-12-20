package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

public class PreviousUtil {

    public static void addToPrevious(DirectionQuery query,
                                     Collection<ContentKey> toAdd,
                                     SortedSet<ContentKey> orderedKeys) {
        Collection<ContentKey> contentKeys = new TreeSet<>(Collections.reverseOrder());
        contentKeys.addAll(toAdd);
        DateTime time = TimeUtil.time(query.isStable());
        for (ContentKey contentKey : contentKeys) {
            if (contentKey.compareTo(query.getContentKey()) < 0 && contentKey.getTime().isBefore(time)) {
                orderedKeys.add(contentKey);
                if (orderedKeys.size() >= query.getCount()) {
                    return;
                }
            }
        }
    }
}
