package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.HubUtils;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
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
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.StaleUtil.addStaleEntities;

@Path("/internal/channel")
public class InternalChannelResource {

    public final static String DESCRIPTION = "Delete, refresh, and check the staleness of channels.";
    private final static Logger logger = LoggerFactory.getLogger(InternalChannelResource.class);
    private final static Long DEFAULT_STALE_AGE = TimeUnit.DAYS.toMinutes(1);

    private final Dao<ChannelConfig> channelConfigDao;
    private final HubUtils hubUtils;
    private final HubProperties hubProperties;
    private final ChannelService channelService;
    private final ChannelResource channelResource;
    private final ObjectMapper mapper;

    @Context
    private UriInfo uriInfo;

    @Inject
    InternalChannelResource(@Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                            ChannelService channelService,
                            ChannelResource channelResource,
                            ObjectMapper mapper,
                            HubUtils hubUtils,
                            HubProperties hubProperties)
    {
        this.channelConfigDao = channelConfigDao;
        this.channelService = channelService;
        this.channelResource = channelResource;
        this.mapper = mapper;
        this.hubUtils = hubUtils;
        this.hubProperties = hubProperties;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response get(@Context UriInfo uriInfo) throws Exception {
        ObjectNode root = mapper.createObjectNode();
        root.put("description", DESCRIPTION);

        ObjectNode directions = root.putObject("directions");
        directions.put("delete", "HTTP DELETE to /internal/channel/{name} to override channel protection in an unprotected cluster.");
        directions.put("refresh", "HTTP GET to /internal/channel/refresh to refresh Channel Cache within the hub cluster.");
        ObjectNode stale = directions.putObject("stale");
        stale.put("by age", "HTTP GET to /internal/channel/stale/{age} to list channels with no inserts for {age} minutes.");
        stale.put("by age, owner", "HTTP GET to /internal/channel/stale/{age}/{owner} to list channels with no inserts for {age} minutes, owned by {owner}.");

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
            return channelResource.notFound(channelName);
        }
        if (hubProperties.isProtected()) {
            logger.info("using internal localhost only to delete {}", channelName);
            return LocalHostOnly.getResponse(uriInfo, () -> channelResource.deletion(channelName));
        }
        logger.info("using internal delete {}", channelName);
        return channelResource.deletion(channelName);
    }

    @GET
    @Path("/stale/{age}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response stale(@PathParam("age") int age) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getRequestUri().toString());
        addStaleEntities(root, age, (staleCutoff) -> {
            Map<DateTime, URI> staleChannels = new TreeMap<>();
            channelService.getChannels().forEach(channelConfig -> {
                Optional<ContentKey> optionalContentKey = channelService.getLatest(channelConfig.getDisplayName(), false);
                if (!optionalContentKey.isPresent()) return;

                ContentKey contentKey = optionalContentKey.get();
                if (contentKey.getTime().isAfter(staleCutoff)) return;

                URI channelURI = constructChannelURI(channelConfig);
                staleChannels.put(contentKey.getTime(), channelURI);
            });
            return staleChannels;
        });
        return Response.ok(root).build();
    }

    private void addLink(ObjectNode node, String key, String value) {
        ObjectNode link = node.putObject(key);
        link.put("href", value);
    }

    private URI constructChannelURI(ChannelConfig channelConfig) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).path("channel").path(channelConfig.getDisplayName()).build();
    }

    @GET
    @Path("/stale/{age}/{owner}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response staleForOwner(@PathParam("age") int age,
                                  @PathParam("owner") String owner) {
        ObjectNode root = mapper.createObjectNode();
        ObjectNode links = root.putObject("_links");
        addLink(links, "self", uriInfo.getRequestUri().toString());
        addStaleEntities(root, age, (staleCutoff) -> {
            Map<DateTime, URI> staleChannels = new TreeMap<>();
            channelService.getChannels().forEach(channelConfig -> {
                Optional<ContentKey> optionalContentKey = channelService.getLatest(channelConfig.getDisplayName(), false);
                if (!optionalContentKey.isPresent()) return;

                ContentKey contentKey = optionalContentKey.get();
                if (contentKey.getTime().isAfter(staleCutoff)) return;

                if (!channelConfig.getOwner().equals(owner)) return;

                URI channelURI = constructChannelURI(channelConfig);
                staleChannels.put(contentKey.getTime(), channelURI);
            });
            return staleChannels;
        });
        return Response.ok(root).build();
    }
}
