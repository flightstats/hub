package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.LocalHostOnly;
import com.flightstats.hub.app.PermissionsChecker;
import com.flightstats.hub.config.ContentProperties;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.events.ContentOutput;
import com.flightstats.hub.events.EventsService;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.rest.PATCH;
import com.flightstats.hub.time.NtpMonitor;
import com.flightstats.hub.util.Sleeper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Optional;

import static com.flightstats.hub.rest.Linked.linked;

/**
 * This resource represents a single channel in the Hub.
 */
@SuppressWarnings("WeakerAccess")
@Slf4j
@Path("/channel/{channel}")
public class ChannelResource {

    private final static String READ_ONLY_FAILURE_MESSAGE = "attempted to %s against /channel on read-only node %s";

    private final ObjectMapper objectMapper;
    private final ChannelService channelService;
    private final ContentRetriever contentRetriever;
    private final NtpMonitor ntpMonitor;
    private final EventsService eventsService;
    private final LinkBuilder linkBuilder;
    private final ContentProperties contentProperties;
    private final PermissionsChecker permissionsChecker;

    @Context
    private UriInfo uriInfo;

    @Inject
    public ChannelResource(ObjectMapper objectMapper,
                           ChannelService channelService,
                           ContentRetriever contentRetriever,
                           NtpMonitor ntpMonitor,
                           EventsService eventsService,
                           LinkBuilder linkBuilder,
                           ContentProperties contentProperties,
                           PermissionsChecker permissionsChecker) {
        this.objectMapper = objectMapper;
        this.channelService = channelService;
        this.contentRetriever = contentRetriever;
        this.ntpMonitor = ntpMonitor;
        this.eventsService = eventsService;
        this.linkBuilder = linkBuilder;
        this.contentProperties = contentProperties;
        this.permissionsChecker = permissionsChecker;
    }

    public Response deletion(@PathParam("channel") String channelName) {
        if (this.channelService.delete(channelName)) {
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            return notFound(channelName);
        }
    }

    public static Response notFound(@PathParam("channel") String channelName) {
        return Response.status(Response.Status.NOT_FOUND).entity("channel " + channelName + " not found").build();
    }

    @SneakyThrows
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannelMetadata(@PathParam("channel") String channelName,
                                       @QueryParam("cached") @DefaultValue("true") boolean cached) throws WebApplicationException {
        log.debug("get channel {}", channelName);

        ChannelConfig channelConfig = channelService.getChannelConfig(channelName, cached)
                .orElseThrow(() -> {
                    log.info("unable to get channel " + channelName);
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                });

        ObjectNode output = this.linkBuilder.buildChannelConfigResponse(channelConfig, uriInfo, channelName);
        return Response.ok(output).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(@PathParam("channel") String channelName, String json) {
        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "createChannel", channelName));
        log.debug("put channel {} {}", channelName, json);
        final Optional<ChannelConfig> oldConfig = channelService.getChannelConfig(channelName, false);
        ChannelConfig channelConfig = ChannelConfig.createFromJsonWithName(json, channelName);
        if (oldConfig.isPresent()) {
            ChannelConfig config = oldConfig.get();
            log.info("using old channel {} {}", config, config.getCreationDate().getTime());
            channelConfig = ChannelConfig.updateFromJson(config, StringUtils.defaultIfBlank(json, "{}"));
        }
        log.info("creating channel {} {}", channelConfig, channelConfig.getCreationDate().getTime());
        channelConfig = channelService.updateChannel(channelConfig, oldConfig.orElse(null), LocalHostOnly.isLocalhost(uriInfo));

        URI channelUri = this.linkBuilder.buildChannelUri(channelConfig.getDisplayName(), uriInfo);
        ObjectNode output = this.linkBuilder.buildChannelConfigResponse(channelConfig, uriInfo, channelName);
        return Response.created(channelUri).entity(output).build();
    }

    @SneakyThrows
    @PATCH
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMetadata(@PathParam("channel") String channelName, String json) throws WebApplicationException {
        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "updateMetadata", channelName));
        log.debug("patch channel {} {}", channelName, json);
        final ChannelConfig oldConfig = channelService.getChannelConfig(channelName, false)
                .orElseThrow(() -> {
                    log.info("unable to patch channel " + channelName);
                    throw new WebApplicationException(Response.Status.NOT_FOUND);
                });

        ChannelConfig newConfig = ChannelConfig.updateFromJson(oldConfig, json);
        newConfig = channelService.updateChannel(newConfig, oldConfig, LocalHostOnly.isLocalhost(uriInfo));

        URI channelUri = this.linkBuilder.buildChannelUri(newConfig.getDisplayName(), uriInfo);
        ObjectNode output = this.linkBuilder.buildChannelConfigResponse(newConfig, uriInfo, channelName);
        return Response.ok(channelUri).entity(output).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertValue(@PathParam("channel") String channelName,
                                @HeaderParam("Content-Length") long contentLength,
                                @HeaderParam("Content-Type") String contentType,
                                @QueryParam("threads") @DefaultValue("3") String threads,
                                @QueryParam("forceWrite") @DefaultValue("false") boolean forceWrite,
                                final InputStream data) {
        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "insertValue", channelName));
        if (!this.contentRetriever.isExistingChannel(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        final long start = System.currentTimeMillis();
        final Content content = Content.builder()
                .withContentType(contentType)
                .withContentLength(contentLength)
                .withStream(data)
                .withThreads(Integer.parseInt(threads))
                .build();
        try {
            final ContentKey contentKey = channelService.insert(channelName, content);
            log.trace("posted {}", contentKey);
            final InsertedContentKey insertionResult = new InsertedContentKey(contentKey);
            final URI payloadUri = this.linkBuilder.buildItemUri(contentKey, uriInfo.getAbsolutePath());
            final Linked<InsertedContentKey> linkedResult = linked(insertionResult)
                    .withLink("channel", this.linkBuilder.buildChannelUri(channelName, uriInfo))
                    .withLink("self", payloadUri)
                    .build();

            final Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
            builder.entity(linkedResult);
            builder.location(payloadUri);
            ActiveTraces.getLocal().log(1000, false, log);
            final long time = System.currentTimeMillis() - start;
            int postTimeBuffer = ntpMonitor.getPostTimeBuffer();
            if (time < postTimeBuffer) {
                Sleeper.sleep(postTimeBuffer - time);
            }
            return builder.build();
        } catch (ContentTooLargeException e) {
            return Response.status(413).entity(e.getMessage()).build();
        } catch (Exception e) {
            String key = "";
            if (content.getContentKey().isPresent()) {
                key = content.getContentKey().get().toString();
            }
            log.warn("unable to POST to " + channelName + " key " + key, e);
            throw e;
        }
    }

    @POST
    @Consumes("multipart/*")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/batch")
    public Response insertBatch(@PathParam("channel") final String channelName,
                                @HeaderParam("Content-Type") final String contentType,
                                final InputStream data) {

        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "insertBatch", channelName));
        return insertBulk(channelName, contentType, data);
    }

    @POST
    @Consumes("multipart/*")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/bulk")
    public Response insertBulk(@PathParam("channel") final String channelName,
                               @HeaderParam("Content-Type") final String contentType,
                               final InputStream data) {
        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "insertBulk", channelName));
        try {
            final BulkContent content = BulkContent.builder()
                    .isNew(true)
                    .contentType(contentType)
                    .stream(data)
                    .channel(channelName)
                    .build();
            final Collection<ContentKey> keys = channelService.insert(content);
            log.trace("posted {}", keys);
            final ObjectNode root = objectMapper.createObjectNode();
            final ObjectNode links = root.putObject("_links");
            final ObjectNode self = links.putObject("self");
            if (keys.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                final ContentKey first = keys.iterator().next();
                final ContentKey trimmedKey = new ContentKey(first.getTime(), first.getHash().substring(0, 6)
                        + "/next/" + keys.size() + "?stable=false");
                final URI payloadUri = this.linkBuilder.buildItemUri(trimmedKey, this.linkBuilder.buildChannelUri(channelName, uriInfo));
                self.put("href", payloadUri.toString());
                final ArrayNode uris = links.putArray("uris");
                final URI channelUri = this.linkBuilder.buildChannelUri(channelName, uriInfo);
                for (ContentKey key : keys) {
                    URI uri = this.linkBuilder.buildItemUri(key, channelUri);
                    uris.add(uri.toString());
                }
                return Response.created(payloadUri).entity(root).build();
            }
        } catch (ContentTooLargeException e) {
            return Response.status(413).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.warn("unable to bulk POST to " + channelName, e);
            throw e;
        }
    }

    @GET
    @Path("/events")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput getEvents(@PathParam("channel") String channel,
                                 @HeaderParam("Last-Event-ID") String lastEventId) {
        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "getEvents", channel));
        try {
            log.info("starting events for {} at {}", channel, lastEventId);
            ContentKey contentKey = new ContentKey();
            final ContentKey fromUrl = ContentKey.fromFullUrl(lastEventId);
            if (fromUrl != null) {
                contentKey = fromUrl;
            } else if (contentRetriever.isReplicating(channel)) {
                Optional<ContentKey> latest = contentRetriever.getLatest(channel, true);
                if (latest.isPresent()) {
                    contentKey = latest.get();
                }
            }
            EventOutput eventOutput = new EventOutput();
            eventsService.register(new ContentOutput(channel, eventOutput, contentKey, uriInfo.getBaseUri(), linkBuilder));
            return eventOutput;
        } catch (Exception e) {
            log.warn("unable to events to " + channel, e);
            throw e;
        }
    }

    @DELETE
    public Response delete(@PathParam("channel") final String channelName) throws Exception {
        permissionsChecker.checkReadOnlyPermission(String.format(READ_ONLY_FAILURE_MESSAGE, "delete", channelName));
        Optional<ChannelConfig> optionalChannelConfig = channelService.getChannelConfig(channelName, false);
        if (!optionalChannelConfig.isPresent()) {
            return notFound(channelName);
        }
        if (contentProperties.isChannelProtectionEnabled() || optionalChannelConfig.get().isProtect()) {
            log.info("using localhost only to delete {}", channelName);
            return LocalHostOnly.getResponse(uriInfo, () -> deletion(channelName));
        }
        log.info("using normal delete {}", channelName);
        return deletion(channelName);
    }

}
