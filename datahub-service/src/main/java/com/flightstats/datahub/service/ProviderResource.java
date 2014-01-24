package com.flightstats.datahub.service;

import com.codahale.metrics.annotation.ExceptionMetered;
import com.codahale.metrics.annotation.Timed;
import com.flightstats.datahub.app.config.metrics.PerChannelThroughput;
import com.flightstats.datahub.app.config.metrics.PerChannelTimed;
import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.Content;
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
 * This is a convenience interface for external data Providers.
 * It supports automatic channel creation and does not return links they can not access.
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
    @PerChannelTimed(operationName = "insert", channelNameParameter = "channelName")
    @PerChannelThroughput(operationName = "insertBytes", channelNameParameter = "channelName")
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertValue(@HeaderParam("channelName") final String channelName,
                                @HeaderParam("Content-Type") final String contentType,
                                @HeaderParam(Headers.LANGUAGE) final String contentLanguage,
                                final byte[] data) throws Exception {
        if (!channelService.channelExists(channelName)) {
            logger.info("creating new Provider channel " + channelName);
            ChannelConfiguration configuration = ChannelConfiguration.builder()
                    .withName(channelName)
                    .withType(ChannelConfiguration.ChannelType.Sequence)
                    .build();
            channelService.createChannel(configuration);
        }

        if (data.length > maxPayloadSizeBytes) {
            return Response.status(413).entity("Max payload size is " + maxPayloadSizeBytes + " bytes.").build();
        }

        Content content = Content.builder().withContentLanguage(contentLanguage)
                .withContentType(contentType)
                .withData(data).build();
        channelService.insert(channelName, content);

        return Response.status(Response.Status.OK).build();
    }


}
