package com.flightstats.hub.channel;

import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;

/**
 * This is a convenience interface for external data Providers.
 * It supports automatic channel creation and does not return links they can not access.
 */
@SuppressWarnings("WeakerAccess")
@Path("/provider")
public class ProviderResource {
    private final static Logger logger = LoggerFactory.getLogger(ProviderResource.class);

    private final static ChannelService channelService = HubProvider.getInstance(ChannelService.class);

    protected void ensureChannel(String channelName) {
        if (!channelService.channelExists(channelName)) {
            logger.info("creating new Provider channel " + channelName);
            ChannelConfig configuration = ChannelConfig.builder()
                    .name(channelName)
                    .build();
            channelService.createChannel(configuration);
        }
    }

    @POST
    @Produces(MediaType.TEXT_PLAIN)
    public Response insertValue(@HeaderParam("channelName") final String channelName,
                                @HeaderParam("Content-Type") final String contentType,
                                final InputStream data) throws Exception {

        ensureChannel(channelName);

        Content content = Content.builder()
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

    @POST
    @Consumes("multipart/*")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/bulk")
    public Response insertBulk(@HeaderParam("channelName") final String channelName,
                               @HeaderParam("Content-Type") final String contentType,
                               final InputStream data) throws Exception {
        try {
            ensureChannel(channelName);

            BulkContent content = BulkContent.builder()
                    .isNew(true)
                    .contentType(contentType)
                    .stream(data)
                    .channel(channelName)
                    .build();

            Collection<ContentKey> keys = channelService.insert(content);
            logger.trace("posted {}", keys);
            return Response.status(Response.Status.OK).build();
        } catch (ContentTooLargeException e) {
            return Response.status(413).entity(e.getMessage()).build();
        } catch (Exception e) {
            logger.warn("unable to bulk POST to " + channelName, e);
            throw e;
        }
    }


}
