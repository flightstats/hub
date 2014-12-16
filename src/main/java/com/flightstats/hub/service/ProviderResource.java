package com.flightstats.hub.service;

import com.flightstats.hub.app.config.metrics.EventTimed;
import com.flightstats.hub.app.config.metrics.PerChannelTimed;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfiguration;
import com.flightstats.hub.model.Content;
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
    @EventTimed(name = "provider.ALL.post")
    @PerChannelTimed(operationName = "post", channelNameParameter = "channelName")
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertValue(@HeaderParam("channelName") final String channelName,
                                @HeaderParam("Content-Type") final String contentType,
                                @HeaderParam(Headers.LANGUAGE) final String contentLanguage,
                                final byte[] data) throws Exception {
        if (!channelService.channelExists(channelName)) {
            logger.info("creating new Provider channel " + channelName);
            ChannelConfiguration configuration = ChannelConfiguration.builder()
                    .withName(channelName)
                    .build();
            channelService.createChannel(configuration);
        }

        if (data.length > maxPayloadSizeBytes) {
            return Response.status(413).entity("Max payload size is " + maxPayloadSizeBytes + " bytes.").build();
        }

        Content content = Content.builder().withContentLanguage(contentLanguage)
                .withContentType(contentType)
                .withData(data).build();
        try {
            channelService.insert(channelName, content);
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            String key = "";
            if (content.getContentKey().isPresent()) {
                key = content.getContentKey().get().toString();
            }
            logger.warn("unable to POST to " + channelName + " key " + key, e);
            throw e;
        }
    }


}
