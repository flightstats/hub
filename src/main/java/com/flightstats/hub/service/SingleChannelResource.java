package com.flightstats.hub.service;

import com.flightstats.hub.app.config.PATCH;
import com.flightstats.hub.app.config.metrics.EventTimed;
import com.flightstats.hub.app.config.metrics.PerChannelTimed;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.InsertedContentKey;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.InputStream;
import java.net.URI;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents a single channel in the Hub.
 */
@Path("/channel/{channelName}")
public class SingleChannelResource {
    private final static Logger logger = LoggerFactory.getLogger(SingleChannelResource.class);
    private final ChannelService channelService;
    private final ChannelLinkBuilder linkBuilder;
    private final Integer maxPayloadSizeBytes;
    private final UriInfo uriInfo;

    @Inject
    public SingleChannelResource(ChannelService channelService, ChannelLinkBuilder linkBuilder,
                                 @Named("maxPayloadSizeBytes") Integer maxPayloadSizeBytes, UriInfo uriInfo) {
        this.channelService = channelService;
        this.linkBuilder = linkBuilder;
        this.maxPayloadSizeBytes = maxPayloadSizeBytes;
        this.uriInfo = uriInfo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getChannelMetadata(@PathParam("channelName") String channelName) {
        if (noSuchChannel(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        ChannelConfiguration config = channelService.getChannelConfiguration(channelName);
        URI channelUri = linkBuilder.buildChannelUri(config, uriInfo);
        Linked<ChannelConfiguration> linked = linkBuilder.buildChannelLinks(config, channelUri);

        return Response.ok(channelUri).entity(linked).build();
    }

    @PUT
    @EventTimed(name = "channels.put")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createChannel(@PathParam("channelName") String channelName, String json) throws Exception {
        ChannelConfiguration oldConfig = channelService.getChannelConfiguration(channelName);
        ChannelConfiguration channelConfiguration = ChannelConfiguration.fromJson(json);
        if (oldConfig != null) {
            channelConfiguration = ChannelConfiguration.builder()
                    .withChannelConfiguration(oldConfig)
                    .withUpdateJson(json)
                    .build();
        }
        channelConfiguration = channelService.updateChannel(channelConfiguration);
        URI channelUri = linkBuilder.buildChannelUri(channelConfiguration, uriInfo);
        return Response.created(channelUri).entity(
                linkBuilder.buildChannelLinks(channelConfiguration, channelUri))
                .build();
    }

    @PATCH
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMetadata( @PathParam("channelName") String channelName, String json) throws Exception {
        if (noSuchChannel(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        ChannelConfiguration oldConfig = channelService.getChannelConfiguration(channelName);
        ChannelConfiguration newConfig = ChannelConfiguration.builder()
                .withChannelConfiguration(oldConfig)
                .withUpdateJson(json)
                .build();

        newConfig = channelService.updateChannel(newConfig);

        URI channelUri = linkBuilder.buildChannelUri(newConfig, uriInfo);
        Linked<ChannelConfiguration> linked = linkBuilder.buildChannelLinks(newConfig, channelUri);
        return Response.ok(channelUri).entity(linked).build();
    }

    @POST
    @EventTimed(name = "channel.ALL.post")
    @PerChannelTimed(operationName = "post", channelNameParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertValue(@PathParam("channelName") final String channelName, @HeaderParam("Content-Type") final String contentType,
                                @HeaderParam("Content-Language") final String contentLanguage, @HeaderParam("User") final String user,
                                final InputStream data) throws Exception {
        if (noSuchChannel(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        //todo - gfm - 12/22/14 - where to handle max length?
        /*if (data.length > maxPayloadSizeBytes) {
            return Response.status(413).entity("Max payload size is " + maxPayloadSizeBytes + " bytes.").build();
        }*/
        Content content = Content.builder()
                .withContentLanguage(contentLanguage)
                .withContentType(contentType)
                .withStream(data)
                .withUser(user)
                .build();
        try {
            ContentKey contentKey = channelService.insert(channelName, content);
            InsertedContentKey insertionResult = new InsertedContentKey(contentKey);
            URI payloadUri = linkBuilder.buildItemUri(contentKey, uriInfo.getRequestUri());
            Linked<InsertedContentKey> linkedResult = linked(insertionResult)
                    .withLink("channel", linkBuilder.buildChannelUri(channelName, uriInfo))
                    .withLink("self", payloadUri)
                    .build();

            Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
            builder.entity(linkedResult);
            builder.location(payloadUri);
            ChannelLinkBuilder.addOptionalHeader(Headers.USER, content.getUser(), builder);
            content.logTraces();
            return builder.build();
        } catch (Exception e) {
            String key = "";
            if (content.getContentKey().isPresent()) {
                key = content.getContentKey().get().toString();
            }
            logger.warn("unable to POST to " + channelName + " key " + key, e);
            throw e;
        }
    }

    @DELETE
    public Response delete(@PathParam("channelName") final String channelName) throws Exception {
        if (channelService.delete(channelName)) {
            return Response.status(Response.Status.ACCEPTED).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("channel " + channelName + " not found").build();
        }
    }

    private boolean noSuchChannel(final String channelName) {
        return !channelService.channelExists(channelName);
    }

}
