package com.flightstats.hub.dao;

import com.flightstats.hub.model.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class TagService {
    private final static Logger logger = LoggerFactory.getLogger(TagService.class);

    @Inject
    private ChannelService channelService;

    public Iterable<ChannelConfig> getChannels(String tag) {
        return channelService.getChannels(tag);
    }

    public Iterable<String> getTags() {
        return channelService.getTags();
    }

    public Collection<ChannelContentKey> queryByTime(TimeQuery timeQuery) {
        Iterable<ChannelConfig> channels = getChannels(timeQuery.getTagName());

        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        //todo - gfm - 6/19/15 - this should be multi-threaded
        for (ChannelConfig channel : channels) {
            Collection<ContentKey> contentKeys = channelService.queryByTime(timeQuery.withChannelName(channel.getName()));
            for (ContentKey contentKey : contentKeys) {
                orderedKeys.add(new ChannelContentKey(channel.getName(), contentKey));
            }
        }

        return orderedKeys;
    }

    public Collection<ChannelContentKey> getKeys(DirectionQuery query) {
        Iterable<ChannelConfig> channels = getChannels(query.getTagName());

        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        //todo - gfm - 6/19/15 - this should be multi-threaded
        for (ChannelConfig channel : channels) {
            Collection<ContentKey> contentKeys = channelService.getKeys(query.withChannelName(channel.getName()));
            for (ContentKey contentKey : contentKeys) {
                orderedKeys.add(new ChannelContentKey(channel.getName(), contentKey));
            }
        }

        Stream<ChannelContentKey> stream = orderedKeys.stream();
        if (!query.isNext()) {
            Collection<ChannelContentKey> contentKeys = new TreeSet<>(Collections.reverseOrder());
            contentKeys.addAll(orderedKeys);
            stream = contentKeys.stream();
        }

        return stream
                .limit(query.getCount())
                .collect(Collectors.toCollection(TreeSet::new));
    }
}
