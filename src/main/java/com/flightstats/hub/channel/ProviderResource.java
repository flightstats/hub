package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.aws.ContentRetriever;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import lombok.extern.slf4j.Slf4j;
import org.owasp.encoder.Encode;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Collection;

/**
 * This is a convenience interface for external data Providers.
 * It supports automatic channel creation and does not return links they can not access.
 */
@Slf4j
@Path("/provider")
public class ProviderResource {
    private final ChannelService channelService;
    private final ContentRetriever contentRetriever;

    @Inject
    public ProviderResource(ChannelService channelService,
                            ContentRetriever contentRetriever) {
        this.channelService = channelService;
        this.contentRetriever = contentRetriever;
    }

    private void ensureChannel(String channelName) {
        if (!contentRetriever.isExistingChannel(channelName)) {
            log.info("creating new Provider channel " + channelName);
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
                                final InputStream data) {

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
            log.warn("unable to POST to " + channelName + " key " + key, e);
            throw e;
        }
    }

    @POST
    @Consumes("multipart/*")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/bulk")
    public Response insertBulk(@HeaderParam("channelName") String channelName,
                               @HeaderParam("Content-Type") String contentType,
                               final InputStream data){
        try {

            channelName = Encode.forHtml(channelName);
            contentType = Encode.forHtml(contentType);

            ensureChannel(channelName);

            BulkContent content = BulkContent.builder()
                    .isNew(true)
                    .contentType(contentType)
                    .stream(data)
                    .channel(channelName)
                    .build();

            final Collection<ContentKey> keys = channelService.insert(content);
            log.trace("posted {}", keys);
            return Response.status(Response.Status.OK).build();
        } catch (ContentTooLargeException e) {
            return Response.status(413).entity(e.getMessage()).build();
        } catch (Exception e) {
            log.warn("unable to bulk POST to {}", channelName, e);
            throw e;
        }
    }

}
