package com.flightstats.hub.dao;

import com.flightstats.hub.channel.ChannelEarliestResource;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.TimeQuery;
import com.google.inject.Singleton;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class TagService {

    private ChannelService channelService;
    private ContentRetriever contentRetriever;

    @Inject
    public TagService(ChannelService channelService, ContentRetriever contentRetriever) {
        this.channelService = channelService;
        this.contentRetriever = contentRetriever;
    }

    public Iterable<ChannelConfig> getChannels(String tag) {
        return channelService.getChannels(tag, true);
    }

    public Iterable<String> getTags() {
        return channelService.getTags();
    }

    public SortedSet<ChannelContentKey> queryByTime(TimeQuery timeQuery) {
        final Iterable<ChannelConfig> channels = getChannels(timeQuery.getTagName());
        final SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (ChannelConfig channel : channels) {
            final Collection<ContentKey> contentKeys = contentRetriever.queryByTime(timeQuery.withChannelName(channel.getDisplayName()));
            for (ContentKey contentKey : contentKeys) {
                orderedKeys.add(new ChannelContentKey(channel.getDisplayName(), contentKey));
            }
        }
        return orderedKeys;
    }

    public SortedSet<ChannelContentKey> getKeys(DirectionQuery query) {
        final Iterable<ChannelConfig> channels = getChannels(query.getTagName());
        final SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        final Traces traces = ActiveTraces.getLocal();
        for (ChannelConfig channel : channels) {
            traces.add("query for channel", channel.getDisplayName());
            final Collection<ContentKey> contentKeys = contentRetriever.query(query.withChannelName(channel.getDisplayName()));
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
        final Iterable<ChannelConfig> channels = getChannels(tagQuery.getTagName());
        final SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (ChannelConfig channel : channels) {
            Optional<ContentKey> contentKey = contentRetriever.getLatest(tagQuery.withChannelName(channel.getDisplayName()));
            contentKey.ifPresent(contentKey1 -> orderedKeys.add(new ChannelContentKey(channel.getDisplayName(), contentKey1)));
        }
        if (orderedKeys.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(orderedKeys.last());
        }
    }

    public SortedSet<ChannelContentKey> getEarliest(DirectionQuery tagQuery) {
        final Iterable<ChannelConfig> channels = getChannels(tagQuery.getTagName());
        final Traces traces = ActiveTraces.getLocal();
        traces.add("TagService.getEarliest", tagQuery.getTagName());
        final SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        for (ChannelConfig channel : channels) {
            DirectionQuery query = ChannelEarliestResource.getDirectionQuery(channel.getDisplayName(), tagQuery.getCount(),
                    tagQuery.isStable(), tagQuery.getLocation().name(), tagQuery.getEpoch().name());
            for (ContentKey contentKey : contentRetriever.query(query)) {
                orderedKeys.add(new ChannelContentKey(channel.getDisplayName(), contentKey));
            }
        }
        traces.add("TagService.getEarliest completed", orderedKeys);
        return orderedKeys;
    }

    public Optional<Content> getValue(ItemRequest itemRequest) {
        final Iterable<ChannelConfig> channels = getChannels(itemRequest.getTag());
        for (ChannelConfig channel : channels) {
            Optional<Content> value = channelService.get(itemRequest.withChannel(channel.getDisplayName()));
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    public ChannelService getChannelService() {
        return channelService;
    }
}
