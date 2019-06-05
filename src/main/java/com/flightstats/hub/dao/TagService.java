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

    private final ChannelService channelService;
    private final ContentRetriever contentRetriever;
    private final LinkBuilder linkBuilder;
    private final BulkBuilder bulkBuilder;
    private final ObjectMapper objectMapper;

    @Inject
    public TagService(ChannelService channelService,
                      ContentRetriever contentRetriever,
                      LinkBuilder linkBuilder,
                      BulkBuilder bulkBuilder,
                      ObjectMapper objectMapper) {
        this.channelService = channelService;
        this.contentRetriever = contentRetriever;
        this.linkBuilder = linkBuilder;
        this.bulkBuilder = bulkBuilder;
        this.objectMapper = objectMapper;
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
            final Collection<ContentKey> contentKeys = contentRetriever.queryByTime(timeQuery.withChannelName(channel.getDisplayName()));
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
        Iterable<ChannelConfig> channels = getChannels(tagQuery.getTagName());
        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
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
        Iterable<ChannelConfig> channels = getChannels(tagQuery.getTagName());
        Traces traces = ActiveTraces.getLocal();
        traces.add("TagService.getEarliest", tagQuery.getTagName());
        SortedSet<ChannelContentKey> orderedKeys = Collections.synchronizedSortedSet(new TreeSet<>());
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
        Iterable<ChannelConfig> channels = getChannels(itemRequest.getTag());
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
        TimeQuery query = TimeQuery.builder()
                .tagName(tag)
                .startTime(startTime)
                .stable(stable)
                .unit(unit)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .build();
        SortedSet<ChannelContentKey> keys = queryByTime(query);
        DateTime current = TimeUtil.time(stable);
        DateTime next = startTime.plus(unit.getDuration());
        DateTime previous = startTime.minus(unit.getDuration());
        String baseUri = uriInfo.getBaseUri() + "tag/" + tag + "/";
        if (bulk) {
            //todo - gfm - order
            return bulkBuilder.buildTag(tag, keys, getChannelService(), uriInfo, accept, (builder) -> {
                if (next.isBefore(current)) {
                    builder.header("Link", "<" + baseUri + unit.format(next) + "?bulk=true&stable=" + stable + ">;rel=\"" + "next" + "\"");
                }
                builder.header("Link", "<" + baseUri + unit.format(previous) + "?bulk=true&stable=" + stable + ">;rel=\"" + "previous" + "\"");
            });
        }
        ObjectNode root = objectMapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        if (next.isBefore(current)) {
            links.putObject("next").put("href", baseUri + unit.format(next) + "?stable=" + stable);
        }
        links.putObject("previous").put("href", baseUri + unit.format(previous) + "?stable=" + stable);
        ArrayNode ids = links.putArray("uris");
        ArrayList<ChannelContentKey> list = new ArrayList<>(keys);
        if (descending) {
            Collections.reverse(list);
        }
        for (ChannelContentKey key : list) {
            final URI channelUri = this.linkBuilder.buildChannelUri(key.getChannel(), uriInfo);
            final URI uri = this.linkBuilder.buildItemUri(key.getContentKey(), channelUri);
            ids.add(uri.toString() + "?tag=" + tag);
        }
        if (trace) {
            ActiveTraces.getLocal().output(root);
        }
        return Response.ok(root).build();
    }

    public Response adjacent(String tag, ContentKey contentKey, boolean stable, boolean next, UriInfo uriInfo, String location, String epoch) {
        DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .startKey(contentKey)
                .next(next)
                .stable(stable)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(1).build();
        Collection<ChannelContentKey> keys = getKeys(query);
        if (keys.isEmpty()) {
            return Response.status(NOT_FOUND).build();
        }
        Response.ResponseBuilder builder = Response.status(SEE_OTHER);
        ChannelContentKey foundKey = keys.iterator().next();
        URI uri = uriInfo.getBaseUriBuilder()
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
        DirectionQuery query = DirectionQuery.builder()
                .tagName(tag)
                .startKey(contentKey)
                .next(next)
                .stable(stable)
                .location(Location.valueOf(location))
                .epoch(Epoch.valueOf(epoch))
                .count(count).build();
        SortedSet<ChannelContentKey> keys = getKeys(query);
        if (bulk) {
            //todo - gfm - order
            return bulkBuilder.buildTag(tag, keys, getChannelService(), uriInfo, accept, (builder) -> {
                String baseUri = uriInfo.getBaseUri() + "tag/" + tag + "/";
                if (!keys.isEmpty()) {
                    builder.header("Link", "<" + baseUri + keys.first().getContentKey().toUrl() + "/previous/" + count + "?bulk=true>;rel=\"" + "previous" + "\"");
                    builder.header("Link", "<" + baseUri + keys.last().getContentKey().toUrl() + "/next/" + count + "?bulk=true>;rel=\"" + "next" + "\"");
                }
            });
        }
        return linkBuilder.directionalTagResponse(tag, keys, count, query, uriInfo, true, trace, descending);
    }
}
