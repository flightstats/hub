package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.app.config.PATCH;
import com.flightstats.datahub.app.config.metrics.PerChannelThroughput;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.flightstats.datahub.model.exception.InvalidRequestException;
import com.flightstats.rest.Linked;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Map;

import static com.flightstats.rest.Linked.linked;

/**
 * This resource represents a single channel in the DataHub.
 */
@Path("/channel/{channelName}")
public class SingleChannelResource {
    private final static Logger logger = LoggerFactory.getLogger(SingleChannelResource.class);
    private final ChannelService channelService;
    private final ChannelHypermediaLinkBuilder linkBuilder;
    private final Integer maxPayloadSizeBytes;
    private final UriInfo uriInfo;
    private static final ObjectMapper mapper = new ObjectMapper();


    @Inject
    public SingleChannelResource(ChannelService channelService, ChannelHypermediaLinkBuilder linkBuilder,
                                 @Named("maxPayloadSizeBytes") Integer maxPayloadSizeBytes, UriInfo uriInfo) {
        this.channelService = channelService;
        this.linkBuilder = linkBuilder;
        this.maxPayloadSizeBytes = maxPayloadSizeBytes;
        this.uriInfo = uriInfo;
    }

    @GET
    @Timed
    @ExceptionMetered
    @PerChannelTimed(operationName = "metadata", channelNameParameter = "channelName")
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

    @PATCH
    @Timed
    @ExceptionMetered
    @PerChannelTimed(operationName = "update", channelNameParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateMetadata( @PathParam("channelName") String channelName, String json) throws Exception {
        if (noSuchChannel(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        Map<String, String> map = mapper.readValue(json, new TypeReference<Map<String, String>>() { });
        if (map.containsKey("type")) {
            throw new InvalidRequestException("{\"error\": \"type can not be changed \"}");
        }
        map.remove("name");
        ChannelConfiguration oldConfig = channelService.getChannelConfiguration(channelName);
        ChannelConfiguration newConfig = ChannelConfiguration.builder()
                .withChannelConfiguration(oldConfig)
                .withMap(map)
                .build();
        newConfig = channelService.updateChannel(newConfig);

        URI channelUri = linkBuilder.buildChannelUri(newConfig, uriInfo);
        Linked<ChannelConfiguration> linked = linkBuilder.buildChannelLinks(newConfig, channelUri);
        return Response.ok(channelUri).entity(linked).build();
    }

    @POST
    @Timed(name = "all-channels.insert")
    @ExceptionMetered
    @PerChannelTimed(operationName = "insert", channelNameParameter = "channelName")
    @PerChannelThroughput(operationName = "insertBytes", channelNameParameter = "channelName")
    @Produces(MediaType.APPLICATION_JSON)
    public Response insertValue(@PathParam("channelName") final String channelName, @HeaderParam("Content-Type") final String contentType,
                                @HeaderParam("Content-Language") final String contentLanguage,
                                final byte[] data) throws Exception {
        if (noSuchChannel(channelName)) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        if (data.length > maxPayloadSizeBytes) {
            return Response.status(413).entity("Max payload size is " + maxPayloadSizeBytes + " bytes.").build();
        }

        Content content = Content.builder().withContentLanguage(contentLanguage)
                .withContentType(contentType)
                .withData(data).build();
        ValueInsertionResult insertionResult = channelService.insert(channelName, content);
        URI payloadUri = linkBuilder.buildItemUri(insertionResult.getKey(), uriInfo.getRequestUri());
        Linked<ValueInsertionResult> linkedResult = linked(insertionResult)
                .withLink("channel", linkBuilder.buildChannelUri(channelName, uriInfo))
                .withLink("self", payloadUri)
                .build();

        Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
        builder.entity(linkedResult);
        builder.location(payloadUri);
        return builder.build();
    }

    @DELETE
    public Response delete(@PathParam("channelName") final String channelName) throws Exception {
        channelService.delete(channelName);
        return Response.ok().build();
    }

    private boolean noSuchChannel(final String channelName) {
        return !channelService.channelExists(channelName);
    }

}
