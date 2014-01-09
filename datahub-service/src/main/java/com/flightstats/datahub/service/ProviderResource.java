package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * This resource represents a single channel in the DataHub.
 * we can have an interface /provider, which only supports POST, can get the channel name from headers,
 * create a new channel if needed, and doesn't return links
 */
@Path("/provider")
public class ProviderResource {
    private final static Logger logger = LoggerFactory.getLogger(ProviderResource.class);
    private final ChannelService channelService;
    private final Integer maxPayloadSizeBytes;

    @Inject
    public ProviderResource(ChannelService channelService,
                            @Named("maxPayloadSizeBytes") Integer maxPayloadSizeBytes) {
        this.channelService = channelService;
        this.maxPayloadSizeBytes = maxPayloadSizeBytes;
    }

    @POST
    @Timed(name = "provider.insert")
    @ExceptionMetered
    //todo - gfm - 1/8/14 - how do we do handle per channel stats?
    //@PerChannelTimed(operationName = "insert", channelNamePathParameter = "channelName")
    //@PerChannelThroughput(operationName = "insertBytes", channelNamePathParameter = "channelName")
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertValue(@HeaderParam("channelName") final String channelName,
                                @HeaderParam("Content-Type") final String contentType,
                                @HeaderParam("Content-Language") final String contentLanguage,
                                final byte[] data) throws Exception {
        if (!channelService.channelExists(channelName)) {
            //todo - gfm - 1/8/14 - create Channel
            ChannelConfiguration configuration = ChannelConfiguration.builder()
                    .withName(channelName)
                    .withType(ChannelConfiguration.ChannelType.Sequence)
                    .build();
            channelService.createChannel(configuration);
        }

        if (data.length > maxPayloadSizeBytes) {
            return Response.status(413).entity("Max payload size is " + maxPayloadSizeBytes + " bytes.").build();
        }

        channelService.insert(channelName, Optional.fromNullable(contentType),
                Optional.fromNullable(contentLanguage), data);

        Response.ResponseBuilder builder = Response.status(Response.Status.OK);
        return builder.build();
    }


}
