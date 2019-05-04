package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.config.ContentProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.HubUtils;
import com.google.inject.TypeLiteral;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import static com.flightstats.hub.util.StaleUtil.addStaleEntities;

@Path("/internal/channel")
@Slf4j
public class InternalChannelResource {

    public static final String DESCRIPTION = "Delete, refresh, and check the staleness of channels.";
    private final static Dao<ChannelConfig> channelConfigDao = HubProvider.getInstance(
            new TypeLiteral<Dao<ChannelConfig>>() {
            }, "ChannelConfig");
    private final static HubUtils hubUtils = HubProvider.getInstance(HubUtils.class);
    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private final static ContentRetriever contentRetriever = HubProvider.getInstance(ContentRetriever.class);
    private final static ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private static final Long DEFAULT_STALE_AGE = TimeUnit.DAYS.toMinutes(1);
    @Context
    private UriInfo uriInfo;
    private final ContentProperties contentProperties;

    @Inject
    public InternalChannelResource(ContentProperties contentProperties) {
        this.contentProperties = contentProperties;
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
        log.info("refreshing all = {}", all);
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

    @SneakyThrows
    @Path("{channel}")
    @DELETE
    public Response delete(@PathParam("channel") final String channelName) throws Exception {
        channelService.getChannelConfig(channelName, false)
                .orElseThrow(() -> {
                    Response errorResponse = ChannelResource.notFound(channelName);
                    throw new WebApplicationException(errorResponse);
                });
        if (contentProperties.isChannelProtectionEnabled()) {
            log.info("using internal localhost only to delete {}", channelName);
            return LocalHostOnly.getResponse(uriInfo, () -> ChannelResource.deletion(channelName));
        }
        log.info("using internal delete {}", channelName);
        return ChannelResource.deletion(channelName);
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
                Optional<ContentKey> optionalContentKey = contentRetriever.getLatest(channelConfig.getDisplayName(), false);
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
                Optional<ContentKey> optionalContentKey = contentRetriever.getLatest(channelConfig.getDisplayName(), false);
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
