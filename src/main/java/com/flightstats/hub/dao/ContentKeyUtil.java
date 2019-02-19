package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.flightstats.hub.util.TimeUtil;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ContentKeyUtil {

    public static SortedSet<ContentKey> filter(Collection<ContentKey> keys, DirectionQuery query) {
        Stream<ContentKey> stream = keys.stream();
        if (query.isNext()) {
            stream = stream.filter(key -> key.compareTo(query.getStartKey()) > 0);
        } else {
            Collection<ContentKey> contentKeys = new TreeSet<>(Collections.reverseOrder());
            contentKeys.addAll(keys);
            stream = contentKeys.stream()
                    .filter(key -> key.compareTo(query.getStartKey()) < 0);
        }
        stream = enforceLimits(query, stream);
        return stream
                .limit(query.getCount())
                .collect(Collectors.toCollection(TreeSet::new));
    }

    static Stream<ContentKey> enforceLimits(Query query, Stream<ContentKey> stream) {
        ChannelConfig channelConfig = query.getChannelConfig();
        if (!channelConfig.isHistorical()) {
            stream = stream.filter(key -> !key.getTime().isBefore(channelConfig.getTtlTime()));
        } else if (query.getEpoch().equals(Epoch.IMMUTABLE)) {
            stream = stream.filter(key -> key.getTime().isAfter(channelConfig.getMutableTime()));
        } else if (query.getEpoch().equals(Epoch.MUTABLE)) {
            stream = stream.filter(key -> !key.getTime().isAfter(channelConfig.getMutableTime()));
        }
        if (query.isStable()) {
            stream = stream.filter(key -> !key.getTime().isAfter(query.getChannelStable()));
        }
        return stream;
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

    public static void convertKeyStrings(String keysString, Collection<ContentKey> contentKeys) {
        if (StringUtils.isNotEmpty(keysString)) {
            String[] keys = keysString.split(",");
            for (String key : keys) {
                contentKeys.add(convertKey(key).get());
            }
        }
    }

    public static Optional<ContentKey> convertKey(String key) {
        if (StringUtils.isNotEmpty(key)) {
            return ContentKey.fromUrl(StringUtils.substringAfter(key, "/"));
        }
        return Optional.empty();
    }
}
