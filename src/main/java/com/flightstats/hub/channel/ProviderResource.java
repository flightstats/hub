package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.rest.Headers;
import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

/**
 * This is a convenience interface for external data Providers.
 * It supports automatic channel creation and does not return links they can not access.
 */
@Path("/provider")
public class ProviderResource {
    private final static Logger logger = LoggerFactory.getLogger(ProviderResource.class);
    private final ChannelService channelService;

    @Inject
    public ProviderResource(ChannelService channelService) {
        this.channelService = channelService;

    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertValue(@HeaderParam("channelName") final String channelName,
                                @HeaderParam("Content-Type") final String contentType,
                                @HeaderParam(Headers.LANGUAGE) final String contentLanguage,
                                final InputStream data) throws Exception {
        if (!channelService.channelExists(channelName)) {
            logger.info("creating new Provider channel " + channelName);
            ChannelConfig configuration = ChannelConfig.builder()
                    .withName(channelName)
                    .build();
            channelService.createChannel(configuration);
        }

        Content content = Content.builder().withContentLanguage(contentLanguage)
                .withContentType(contentType)
                .withStream(data).build();
        try {
            channelService.insert(channelName, content);
            return Response.status(Response.Status.OK).build();
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


}
