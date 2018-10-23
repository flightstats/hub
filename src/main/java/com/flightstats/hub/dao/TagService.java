package com.flightstats.hub.dao;

import com.flightstats.hub.channel.ChannelEarliestResource;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.google.common.base.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class TagService {

    private final ChannelService channelService;

    @Inject
    TagService(ChannelService channelService) {
        this.channelService = channelService;
    }

    public Iterable<ChannelConfig> getChannels(String tag) {
        return channelService.getChannels(tag, true);
    }

    public Iterable<String> getTags() {
        return channelService.getTags();
    }

    public SortedSet<ChannelContentKey> queryByTime(TimeQuery timeQuery) {
        Iterable<ChannelConfig> channels = getChannels(timeQuery.getTagName());
        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (ChannelConfig channel : channels) {
            Collection<ContentKey> contentKeys = channelService.queryByTime(timeQuery.withChannelName(channel.getDisplayName()));
            for (ContentKey contentKey : contentKeys) {
                orderedKeys.add(new ChannelContentKey(channel.getDisplayName(), contentKey));
            }
        }
        return orderedKeys;
    }

    public SortedSet<ChannelContentKey> getKeys(DirectionQuery query) {
        Iterable<ChannelConfig> channels = getChannels(query.getTagName());
        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        Traces traces = ActiveTraces.getLocal();
        for (ChannelConfig channel : channels) {
            traces.add("query for channel", channel.getDisplayName());
            Collection<ContentKey> contentKeys = channelService.query(query.withChannelName(channel.getDisplayName()));
            traces.add("query size for channel", channel.getDisplayName(), contentKeys.size());
            for (ContentKey contentKey : contentKeys) {
                orderedKeys.add(new ChannelContentKey(channel.getDisplayName(), contentKey));
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

    public Optional<ChannelContentKey> getLatest(DirectionQuery tagQuery) {
        Iterable<ChannelConfig> channels = getChannels(tagQuery.getTagName());
        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (ChannelConfig channel : channels) {
            Optional<ContentKey> contentKey = channelService.getLatest(tagQuery.withChannelName(channel.getDisplayName()));
            if (contentKey.isPresent()) {
                orderedKeys.add(new ChannelContentKey(channel.getDisplayName(), contentKey.get()));
            }
        }
        if (orderedKeys.isEmpty()) {
            return Optional.absent();
        } else {
            return Optional.of(orderedKeys.last());
        }
    }

    public SortedSet<ChannelContentKey> getEarliest(DirectionQuery tagQuery) {
        Iterable<ChannelConfig> channels = getChannels(tagQuery.getTagName());
        Traces traces = ActiveTraces.getLocal();
        traces.add("TagService.getEarliest", tagQuery.getTagName());
        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (ChannelConfig channel : channels) {
            DirectionQuery query = ChannelEarliestResource.getDirectionQuery(channel.getDisplayName(), tagQuery.getCount(),
                    tagQuery.isStable(), tagQuery.getLocation().name(), tagQuery.getEpoch().name());
            for (ContentKey contentKey : channelService.query(query)) {
                orderedKeys.add(new ChannelContentKey(channel.getDisplayName(), contentKey));
            }
        }
        traces.add("TagService.getEarliest completed", orderedKeys);
        return orderedKeys;
    }

    public Optional<Content> getValue(ItemRequest itemRequest) {
        Iterable<ChannelConfig> channels = getChannels(itemRequest.getTag());
        for (ChannelConfig channel : channels) {
            Optional<Content> value = channelService.get(itemRequest.withChannel(channel.getDisplayName()));
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
