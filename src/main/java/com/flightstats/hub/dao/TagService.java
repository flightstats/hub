package com.flightstats.hub.dao;

import com.flightstats.hub.channel.ChannelEarliestResource;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
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
        for (ChannelConfig channel : channels) {
            query.getTraces().add("query for channel", channel.getName());
            Collection<ContentKey> contentKeys = channelService.getKeys(query.withChannelName(channel.getName()));
            query.getTraces().add("query size for channel", channel.getName(), contentKeys.size());
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

    public Optional<ChannelContentKey> getLatest(String tag, boolean stable, boolean trace) {
        Iterable<ChannelConfig> channels = getChannels(tag);
        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (ChannelConfig channel : channels) {
            Optional<ContentKey> contentKey = channelService.getLatest(channel.getName(), stable, trace);
            if (contentKey.isPresent()) {
                orderedKeys.add(new ChannelContentKey(channel.getName(), contentKey.get()));
            }
        }
        if (orderedKeys.isEmpty()) {
            return Optional.absent();
        } else {
            return Optional.of(orderedKeys.last());
        }
    }

    public Collection<ChannelContentKey> getEarliest(String tag, int count, boolean stable, boolean trace) {
        Iterable<ChannelConfig> channels = getChannels(tag);
        Traces traces = Traces.getTraces(trace);
        traces.add("earliest for tag", tag);
        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (ChannelConfig channel : channels) {
            DirectionQuery query = ChannelEarliestResource.getDirectionQuery(channel.getName(), count, stable, trace, channelService);
            query.setTraces(traces);
            traces.add("earliest", query);
            Collection<ContentKey> contentKeys = channelService.getKeys(query);
            for (ContentKey contentKey : contentKeys) {
                orderedKeys.add(new ChannelContentKey(channel.getName(), contentKey));
            }
            traces.add("earliest", orderedKeys);
        }
        traces.log(logger);
        return orderedKeys;
    }

    public Optional<Content> getValue(Request request) {
        Iterable<ChannelConfig> channels = getChannels(request.getTag());
        for (ChannelConfig channel : channels) {
            Optional<Content> value = channelService.getValue(request.withChannel(channel.getName()));
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.absent();
    }

    public ChannelService getChannelService() {
        return channelService;
    }
}
