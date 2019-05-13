package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.rest.HalLink;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Consumer;

public class LinkBuilder {

    private final TimeLinkBuilder timeLinkBuilder;
    private final ObjectMapper objectMapper;

    @Inject
    public LinkBuilder(TimeLinkBuilder timeLinkBuilder, ObjectMapper objectMapper) {
        this.timeLinkBuilder = timeLinkBuilder;
        this.objectMapper = objectMapper;
    }

    public URI buildChannelUri(String channelName, UriInfo uriInfo) {
        return uriInfo.getBaseUriBuilder().path("channel").path(channelName).build();
    }

    public URI buildItemUri(ContentPath key, URI channelUri) {
        return buildItemUri(key.toUrl(), channelUri);
    }

    private URI buildItemUri(String key, URI channelUri) {
        return URI.create(channelUri.toString() + "/" + key);
    }

    ObjectNode buildChannelConfigResponse(ChannelConfig config, UriInfo uriInfo, String channel) {
        ObjectNode root = this.objectMapper.createObjectNode();

        root.put("name", config.getDisplayName());
        root.put("allowZeroBytes", config.isAllowZeroBytes());
        root.put("secondaryMetricsReporting", config.isSecondaryMetricsReporting());
        root.put("creationDate", TimeUtil.FORMATTER.print(new DateTime(config.getCreationDate())));
        root.put("description", config.getDescription());
        root.put("maxItems", config.getMaxItems());
        if (config.getMutableTime() != null) {
            root.put("mutableTime", TimeUtil.FORMATTER.print(config.getMutableTime()));
        } else {
            root.put("mutableTime", "");
        }
        root.put("owner", config.getOwner());
        root.put("protect", config.isProtect());
        root.put("replicationSource", config.getReplicationSource());
        root.put("storage", config.getStorage());
        ArrayNode tags = root.putArray("tags");
        config.getTags().forEach(tags::add);
        root.put("ttlDays", config.getTtlDays());
        root.put("keepForever", config.getKeepForever());

        ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getBaseUriBuilder().path("channel").path(channel).build());
        addLink(links, "documentation", uriInfo.getBaseUriBuilder().path("channel").path(channel).path("doc").build());
        addLink(links, "latest", uriInfo.getBaseUriBuilder().path("channel").path(channel).path("latest").build());
        addLink(links, "earliest", uriInfo.getBaseUriBuilder().path("channel").path(channel).path("earliest").build());
        addLink(links, "bulk", uriInfo.getBaseUriBuilder().path("channel").path(channel).path("bulk").build());
        addLink(links, "ws", uriInfo.getBaseUriBuilder().path("channel").path(channel).path("ws").scheme("ws").build());
        addLink(links, "events", uriInfo.getBaseUriBuilder().path("channel").path(channel).path("events").build());
        addLink(links, "time", uriInfo.getBaseUriBuilder().path("channel").path(channel).path("time").build());
        addLink(links, "status", uriInfo.getBaseUriBuilder().path("channel").path(channel).path("status").build());

        return root;
    }

    Linked<?> buildLinks(UriInfo uriInfo, Map<String, URI> nameToUriMap, String name) {
        return buildLinks(nameToUriMap, name, builder ->
                builder.withLink("self", uriInfo.getRequestUri()));
    }

    Linked<?> buildLinks(Map<String, URI> nameToUriMap, String name, Consumer<Linked.Builder> consumer) {
        Linked.Builder responseBuilder = Linked.justLinks();
        consumer.accept(responseBuilder);
        List<HalLink> halLinks = new ArrayList<>(nameToUriMap.size());
        for (Map.Entry<String, URI> entry : nameToUriMap.entrySet()) {
            HalLink link = new HalLink(entry.getKey(), entry.getValue());
            halLinks.add(link);
        }
        responseBuilder.withLinks(name, halLinks);
        return responseBuilder.build();
    }

    UriBuilder uriBuilder(String channel, UriInfo uriInfo) {
        UriBuilder uriBuilder = uriInfo.getBaseUriBuilder()
                .path("channel").path(channel);
        this.timeLinkBuilder.addQueryParams(uriInfo, uriBuilder);
        return uriBuilder;
    }

    URI getDirection(String name, String channel, UriInfo uriInfo, ContentKey key, int count) {
        return uriBuilder(channel, uriInfo)
                .path(key.toUrl())
                .path(name).path("" + count)
                .build();
    }

    Response directionalResponse(SortedSet<ContentKey> keys, int count,
                                 DirectionQuery query, UriInfo uriInfo,
                                 boolean includePrevious, boolean trace, boolean descending) {
        String channel = query.getChannelName();
        ObjectNode root = this.objectMapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        List<ContentKey> list = new ArrayList<>(keys);
        if (list.isEmpty()) {
            ContentKey contentKey = query.getStartKey();
            if (query.isNext()) {
                ObjectNode previous = links.putObject("previous");
                previous.put("href", getDirection("previous", channel, uriInfo, contentKey, count).toString());
            } else {
                ObjectNode next = links.putObject("next");
                next.put("href", getDirection("next", channel, uriInfo, contentKey, count).toString());
            }
        } else {
            ObjectNode next = links.putObject("next");
            next.put("href", getDirection("next", channel, uriInfo, list.get(list.size() - 1), count).toString());
            if (includePrevious) {
                ObjectNode previous = links.putObject("previous");
                previous.put("href", getDirection("previous", channel, uriInfo, list.get(0), count).toString());
            }
        }
        ArrayNode ids = links.putArray("uris");
        URI channelUri = buildChannelUri(channel, uriInfo);
        if (descending) {
            Collections.reverse(list);
        }
        for (ContentKey key : list) {
            URI uri = buildItemUri(key, channelUri);
            ids.add(uri.toString());
        }
        if (trace) {
            ActiveTraces.getLocal().output(root);
        }
        return Response.ok(root).build();
    }

    public Response directionalTagResponse(String tag, SortedSet<ChannelContentKey> keys, int count,
                                           DirectionQuery query, UriInfo uriInfo,
                                           boolean includePrevious, boolean trace, boolean descending) {
        ObjectNode root = this.objectMapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        ObjectNode self = links.putObject("self");
        self.put("href", uriInfo.getRequestUri().toString());
        List<ChannelContentKey> list = new ArrayList<>(keys);
        String baseUri = uriInfo.getBaseUri() + "tag/" + tag + "/";
        if (list.isEmpty()) {
            if (query.isNext()) {
                ObjectNode previous = links.putObject("previous");
                previous.put("href", baseUri + query.getStartKey().toUrl() + "/previous/" + count);
            } else {
                ObjectNode next = links.putObject("next");
                next.put("href", baseUri + query.getStartKey().toUrl() + "/next/" + count);
            }
        } else {
            ObjectNode next = links.putObject("next");
            next.put("href", baseUri + list.get(list.size() - 1).getContentKey().toUrl() + "/next/" + count);
            if (includePrevious) {
                ObjectNode previous = links.putObject("previous");
                previous.put("href", baseUri + list.get(0).getContentKey().toUrl() + "/previous/" + count);
            }
        }
        ArrayNode ids = links.putArray("uris");
        if (descending) {
            Collections.reverse(list);
        }
        for (ChannelContentKey key : list) {
            ids.add(uriInfo.getBaseUri() + key.toUrl() + "?tag=" + tag);
        }
        if (trace) {
            ActiveTraces.getLocal().output(root);
        }
        return Response.ok(root).build();
    }

    public void addLink(ObjectNode parent, String name, URI link) {
        parent.putObject(name).put("href", link.toString());
    }

    URI getUri(String channel, UriInfo uriInfo, TimeUtil.Unit unit, DateTime time) {
        return uriBuilder(channel, uriInfo).path(unit.format(time)).build();
    }
}
