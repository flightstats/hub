package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.HubUtils;
import com.google.common.base.Optional;
import com.google.inject.TypeLiteral;
import org.joda.time.DateTime;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

@Path("/internal/channel")
public class InternalChannelResource {

    public static final String DESCRIPTION = "Delete, refresh, and check the staleness of channels.";
    private final static Logger logger = LoggerFactory.getLogger(InternalChannelResource.class);
    private final static Dao<ChannelConfig> channelConfigDao = HubProvider.getInstance(
            new TypeLiteral<Dao<ChannelConfig>>() {
            }, "ChannelConfig");
    private final static HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);
    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final Long DEFAULT_STALE_AGE = TimeUnit.DAYS.toMinutes(1);
    @Context
    private UriInfo uriInfo;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context UriInfo uriInfo) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);

        ObjectNode directions = root.putObject("directions");
        directions.put("delete", "HTTP DELETE to /internal/channel/{name} to override channel protection in an unprotected cluster.");
        directions.put("refresh", "HTTP GET to /internal/channel/refresh to refresh Channel Cache within the hub cluster.");
        directions.put("stale", "HTTP GET to /internal/channel/stale/{age} to list channels with no inserts for {age} minutes.");

        ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getRequestUri().toString());
        addLink(links, "refresh", uriInfo.getRequestUri().toString() + "/refresh");
        addLink(links, "stale", uriInfo.getRequestUri().toString() + "/stale/" + DEFAULT_STALE_AGE.intValue());

        return Response.ok(root).build();
    }

    @GET
    @Path("/refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refresh(@QueryParam("all") @DefaultValue("true") boolean all) throws Exception {
        logger.info("refreshing all = {}", all);
        if (all) {
            return Response.ok(hubUtils.refreshAll()).build();
        } else {
            if (channelConfigDao.refresh()) {
                return Response.ok(HubHost.getLocalNamePort()).build();
            } else {
                return Response.status(400).entity(HubHost.getLocalNamePort()).build();
            }
        }
    }

    @Path("{channel}")
    @DELETE
    public Response delete(@PathParam("channel") final String channelName) throws Exception {
        ChannelConfig channelConfig = channelService.getChannelConfig(channelName, false);
        if (channelConfig == null) {
            return ChannelResource.notFound(channelName);
        }
        if (HubProperties.isProtected()) {
            logger.info("using internal localhost only to delete {}", channelName);
            return LocalHostOnly.getResponse(uriInfo, () -> ChannelResource.deletion(channelName));
        }
        logger.info("using internal delete {}", channelName);
        return ChannelResource.deletion(channelName);
    }

    @GET
    @Path("/stale/{age}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stale(@PathParam("age") int age) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getRequestUri().toString());
        addStaleChannels(root, age);
        return Response.ok(root).build();
    }

    private void addLink(ObjectNode node, String key, String value) {
        ObjectNode link = node.putObject(key);
        link.put("href", value);
    }

    private void addStaleChannels(ObjectNode root, int age) {
        DateTime staleCutoff = DateTime.now().minusMinutes(age);

        ObjectNode stale = root.putObject("stale");
        stale.put("stale minutes", age);
        stale.put("stale cutoff", staleCutoff.toString());

        Map<Minutes, URI> staleChannels = new TreeMap<>(Collections.reverseOrder());
        channelService.getChannels().forEach(channelConfig -> {
            Optional<ContentKey> optionalContentKey = channelService.getLatest(channelConfig.getName(), false, false);
            if (!optionalContentKey.isPresent()) return;

            ContentKey contentKey = optionalContentKey.get();
            if (contentKey.getTime().isAfter(staleCutoff)) return;

            Minutes channelAge = Minutes.minutesBetween(contentKey.getTime(), DateTime.now());
            URI channelURI = constructChannelURI(channelConfig);
            staleChannels.put(channelAge, channelURI);
        });

        ArrayNode uris = stale.putArray("uris");
        staleChannels.forEach((channelAge, channelURI) -> {
            ObjectNode node = createURINode(channelURI, channelAge);
            uris.add(node);
        });
    }

    private ObjectNode createURINode(URI uri, Minutes age) {
        ObjectNode node = mapper.createObjectNode();
        node.put("age", age.getMinutes());
        node.put("uri", uri.toString());
        return node;
    }

    private URI constructChannelURI(ChannelConfig channelConfig) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).path("channel").path(channelConfig.getName()).build();
    }
}
