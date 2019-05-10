package com.flightstats.hub.dao;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.channel.BulkBuilder;
import com.flightstats.hub.channel.ChannelEarliestResource;
import com.flightstats.hub.channel.LinkBuilder;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.Location;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Slf4j
@Singleton
public class TagService {

    private ChannelService channelService;
    private ContentRetriever contentRetriever;
    private ObjectMapper objectMapper;

    @Inject
    public TagService(ChannelService channelService,
                      ContentRetriever contentRetriever,
                      ObjectMapper objectMapper) {
        this.channelService = channelService;
        this.contentRetriever = contentRetriever;
        this.objectMapper = objectMapper;
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

    public Response getTimeQueryResponse(String tag, DateTime startTime, String location, boolean trace, boolean stable,
                                         TimeUtil.Unit unit, boolean bulk, String accept, UriInfo uriInfo, String epoch, boolean descending) {
        final TimeQuery query = TimeQuery.builder()
                .tagName(tag)
                .startTime(startTime)
                .stable(stable)
                .unit(unit)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .build();
        final SortedSet<ChannelContentKey> keys = queryByTime(query);
        final DateTime current = TimeUtil.time(stable);
        final DateTime next = startTime.plus(unit.getDuration());
        final DateTime previous = startTime.minus(unit.getDuration());
        final String baseUri = uriInfo.getBaseUri() + "tag/" + tag + "/";
        if (bulk) {
            //todo - gfm - order
            return BulkBuilder.buildTag(tag, keys, getChannelService(), uriInfo, accept, (builder) -> {
                if (next.isBefore(current)) {
                    builder.header("Link", "<" + baseUri + unit.format(next) + "?bulk=true&stable=" + stable + ">;rel=\"" + "next" + "\"");
                }
                builder.header("Link", "<" + baseUri + unit.format(previous) + "?bulk=true&stable=" + stable + ">;rel=\"" + "previous" + "\"");
            });
        }
        final ObjectNode root = objectMapper.createObjectNode();
        final ObjectNode links = root.putObject("_links");
        final ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        if (next.isBefore(current)) {
            links.putObject("next").put("href", baseUri + unit.format(next) + "?stable=" + stable);
        }
        links.putObject("previous").put("href", baseUri + unit.format(previous) + "?stable=" + stable);
        final ArrayNode ids = links.putArray("uris");
        final ArrayList<ChannelContentKey> list = new ArrayList<>(keys);
        if (descending) {
            Collections.reverse(list);
        }
        for (ChannelContentKey key : list) {
            final URI channelUri = LinkBuilder.buildChannelUri(key.getChannel(), uriInfo);
            final URI uri = LinkBuilder.buildItemUri(key.getContentKey(), channelUri);
            ids.add(uri.toString() + "?tag=" + tag);
        }
        if (trace) {
            ActiveTraces.getLocal().output(root);
        }
        return Response.ok(root).build();
    }

    public Response adjacent(String tag, ContentKey contentKey, boolean stable, boolean next, UriInfo uriInfo, String location, String epoch) {
        final DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .startKey(contentKey)
                .next(next)
                .stable(stable)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(1).build();
        final Collection<ChannelContentKey> keys = getKeys(query);
        if (keys.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        }
        final Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        final ChannelContentKey foundKey = keys.iterator().next();
        final URI uri = uriInfo.getBaseUriBuilder()
                .path("channel")
                .path(foundKey.getChannel())
                .path(foundKey.getContentKey().toUrl())
                .queryParam("tag", tag)
                .queryParam("stable", stable)
                .build();
        log.trace("returning url {}", uri);
        builder.location(uri);
        return builder.build();
    }

    public Response adjacentCount(String tag, int count, boolean stable, boolean trace, String location,
                                  boolean next, ContentKey contentKey, boolean bulk, String accept, UriInfo uriInfo, String epoch, boolean descending) {
        final DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .startKey(contentKey)
                .next(next)
                .stable(stable)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(count).build();
        final SortedSet<ChannelContentKey> keys = getKeys(query);
        if (bulk) {
            //todo - gfm - order
            return BulkBuilder.buildTag(tag, keys, getChannelService(), uriInfo, accept, (builder) -> {
                String baseUri = uriInfo.getBaseUri() + "tag/" + tag + "/";
                if (!keys.isEmpty()) {
                    builder.header("Link", "<" + baseUri + keys.first().getContentKey().toUrl() + "/previous/" + count + "?bulk=true>;rel=\"" + "previous" + "\"");
                    builder.header("Link", "<" + baseUri + keys.last().getContentKey().toUrl() + "/next/" + count + "?bulk=true>;rel=\"" + "next" + "\"");
                }
            });
        }
        return LinkBuilder.directionalTagResponse(tag, keys, count, query, objectMapper, uriInfo, true, trace, descending);
    }
}
