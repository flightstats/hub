package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.events.ContentOutput;
import com.flightstats.hub.events.EventsService;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.NewRelicIgnoreTransaction;
import com.flightstats.hub.model.*;
import com.flightstats.hub.rest.Linked;
import com.flightstats.hub.rest.PATCH;
import com.flightstats.hub.time.NtpMonitor;
import com.flightstats.hub.util.Sleeper;
import com.google.common.base.Optional;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.SseFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;

import static com.flightstats.hub.rest.Linked.linked;

/**
 * This resource represents a single channel in the Hub.
 */
@SuppressWarnings("WeakerAccess")
@Path("/channel/{channel}")
public class ChannelResource {
    private final static Logger logger = LoggerFactory.getLogger(ChannelResource.class);

    @Context
    private UriInfo uriInfo;

    private ObjectMapper mapper = HubProvider.getInstance(ObjectMapper.class);
    private ChannelService channelService = HubProvider.getInstance(ChannelService.class);
    private NtpMonitor ntpMonitor = HubProvider.getInstance(NtpMonitor.class);
    private EventsService eventsService = HubProvider.getInstance(EventsService.class);

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannelMetadata(@PathParam("channel") String channelName,
                                       @QueryParam("cached") @DefaultValue("true") boolean cached) {
        logger.debug("get channel {}", channelName);
        ChannelConfig config = channelService.getChannelConfig(channelName, cached);
        if (config == null) {
            logger.info("unable to get channel " + channelName);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        URI channelUri = LinkBuilder.buildChannelUri(config, uriInfo);
        Linked<ChannelConfig> linked = LinkBuilder.buildChannelLinks(config, channelUri);

        return Response.ok(channelUri).entity(linked).build();
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(@PathParam("channel") String channelName, String json) throws Exception {
        logger.debug("put channel {} {}", channelName, json);
        ChannelConfig oldConfig = channelService.getChannelConfig(channelName, false);
        ChannelConfig channelConfig = ChannelConfig.fromJsonName(json, channelName);
        if (oldConfig != null) {
            logger.info("using old channel {} {}", oldConfig, oldConfig.getCreationDate().getTime());
            channelConfig = ChannelConfig.builder()
                    .withChannelConfiguration(oldConfig)
                    .withUpdateJson(StringUtils.defaultIfBlank(json, "{}"))
                    .build();
        }
        logger.info("creating channel {} {}", channelConfig, channelConfig.getCreationDate().getTime());
        channelConfig = channelService.updateChannel(channelConfig, oldConfig);
        URI channelUri = LinkBuilder.buildChannelUri(channelConfig, uriInfo);
        return Response.created(channelUri).entity(
                LinkBuilder.buildChannelLinks(channelConfig, channelUri))
                .build();
    }

    @PATCH
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMetadata(@PathParam("channel") String channelName, String json) throws Exception {
        logger.debug("patch channel {} {}", channelName, json);
        ChannelConfig oldConfig = channelService.getChannelConfig(channelName, false);
        if (oldConfig == null) {
            logger.info("unable to patch channel " + channelName);
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        ChannelConfig newConfig = ChannelConfig.builder()
                .withChannelConfiguration(oldConfig)
                .withUpdateJson(json)
                .build();

        newConfig = channelService.updateChannel(newConfig, oldConfig);

        URI channelUri = LinkBuilder.buildChannelUri(newConfig, uriInfo);
        Linked<ChannelConfig> linked = LinkBuilder.buildChannelLinks(newConfig, channelUri);
        return Response.ok(channelUri).entity(linked).build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertValue(@PathParam("channel") final String channelName,
                                @HeaderParam("Content-Type") final String contentType,
                                final InputStream data) throws Exception {
        if (!channelService.channelExists(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        long start = System.currentTimeMillis();
        Content content = Content.builder()
                .withContentType(contentType)
                .withStream(data)
                .build();
        try {
            ContentKey contentKey = channelService.insert(channelName, content);
            logger.trace("posted {}", contentKey);
            InsertedContentKey insertionResult = new InsertedContentKey(contentKey);
            URI payloadUri = LinkBuilder.buildItemUri(contentKey, uriInfo.getRequestUri());
            Linked<InsertedContentKey> linkedResult = linked(insertionResult)
                    .withLink("channel", LinkBuilder.buildChannelUri(channelName, uriInfo))
                    .withLink("self", payloadUri)
                    .build();

            Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
            builder.entity(linkedResult);
            builder.location(payloadUri);
            ActiveTraces.getLocal().logSlow(1000, logger);
            long time = System.currentTimeMillis() - start;
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
            logger.warn("unable to POST to " + channelName + " key " + key, e);
            throw e;
        }
    }

    @POST
    @Consumes("multipart/*")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/batch")
    public Response insertBatch(@PathParam("channel") final String channelName,
                                @HeaderParam("Content-Type") final String contentType,
                                final InputStream data) throws Exception {
        return insertBulk(channelName, contentType, data);
    }

    @POST
    @Consumes("multipart/*")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/bulk")
    public Response insertBulk(@PathParam("channel") final String channelName,
                               @HeaderParam("Content-Type") final String contentType,
                               final InputStream data) throws Exception {
        try {
            BulkContent content = BulkContent.builder()
                    .isNew(true)
                    .contentType(contentType)
                    .stream(data)
                    .channel(channelName)
                    .build();
            Collection<ContentKey> keys = channelService.insert(content);
            logger.trace("posted {}", keys);
            ObjectNode root = mapper.createObjectNode();
            ObjectNode links = root.putObject("_links");
            ObjectNode self = links.putObject("self");
            if (keys.isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                ContentKey first = keys.iterator().next();
                ContentKey trimmedKey = new ContentKey(first.getTime(), first.getHash().substring(0, 6)
                        + "/next/" + keys.size() + "?stable=false");
                URI payloadUri = LinkBuilder.buildItemUri(trimmedKey, LinkBuilder.buildChannelUri(channelName, uriInfo));
                self.put("href", payloadUri.toString());
                ArrayNode uris = links.putArray("uris");
                URI channelUri = LinkBuilder.buildChannelUri(channelName, uriInfo);
                for (ContentKey key : keys) {
                    URI uri = LinkBuilder.buildItemUri(key, channelUri);
                    uris.add(uri.toString());
                }
                return Response.created(payloadUri).entity(root).build();
            }
        } catch (ContentTooLargeException e) {
            return Response.status(413).entity(e.getMessage()).build();
        } catch (Exception e) {
            logger.warn("unable to bulk POST to " + channelName, e);
            throw e;
        }
    }

    @GET
    @Path("/events")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    @NewRelicIgnoreTransaction
    public EventOutput getEvents(@PathParam("channel") String channel, @HeaderParam("Last-Event-ID") String lastEventId) throws Exception {
        try {
            logger.info("starting events for {} at {}", channel, lastEventId);
            ContentKey contentKey = new ContentKey();
            ContentKey fromUrl = ContentKey.fromFullUrl(lastEventId);
            if (fromUrl != null) {
                contentKey = fromUrl;
            } else if (channelService.isReplicating(channel)) {
                Optional<ContentKey> latest = channelService.getLatest(channel, true, false);
                if (latest.isPresent()) {
                    contentKey = latest.get();
                }
            }
            EventOutput eventOutput = new EventOutput();
            eventsService.register(new ContentOutput(channel, eventOutput, contentKey, uriInfo.getBaseUri()));
            return eventOutput;
        } catch (Exception e) {
            logger.warn("unable to events to " + channel, e);
            throw e;
        }
    }

    @DELETE
    public Response delete(@PathParam("channel") final String channelName) throws Exception {
        if (HubProperties.getProperty("hub.allow.channel.deletion", false)) {
            return deletion(channelName);
        }
        if (uriInfo.getBaseUri().toString().contains("localhost")) {
            return deletion(channelName);
        } else {
            logger.info("deletions are not allowed");
            return Response.status(405).build();
        }
    }

    private Response deletion(@PathParam("channel") String channelName) {
        if (channelService.delete(channelName)) {
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("channel " + channelName + " not found").build();
        }
    }

}
