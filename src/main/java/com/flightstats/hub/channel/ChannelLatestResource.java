package com.flightstats.hub.channel;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import com.google.inject.Inject;
import org.apache.commons.lang3.RandomStringUtils;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;

import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static javax.ws.rs.core.Response.Status.SEE_OTHER;

@Path("/channel/{channel: .*}/latest")
public class ChannelLatestResource {

    public static final String CRLF = "\r\n";
    @Inject
    private UriInfo uriInfo;
    @Inject
    private ChannelService channelService;
    @Inject
    private ObjectMapper mapper;
    @Inject
    private TagLatestResource tagLatestResource;

    @GET
    public Response getLatest(@PathParam("channel") String channel,
                              @QueryParam("stable") @DefaultValue("true") boolean stable,
                              @QueryParam("trace") @DefaultValue("false") boolean trace,
                              @QueryParam("tag") String tag) {
        if (tag != null) {
            return tagLatestResource.getLatest(tag, stable, trace);
        }
        Optional<ContentKey> latest = channelService.getLatest(channel, stable, trace);
        if (latest.isPresent()) {
            return Response.status(SEE_OTHER)
                    .location(URI.create(uriInfo.getBaseUri() + "channel/" + channel + "/" + latest.get().toUrl()))
                    .build();
        } else {
            return Response.status(NOT_FOUND).build();
        }
    }

    @GET
    @Path("/{count}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getLatestCount(@PathParam("channel") String channel,
                                   @PathParam("count") int count,
                                   @QueryParam("stable") @DefaultValue("true") boolean stable,
                                   @QueryParam("trace") @DefaultValue("false") boolean trace,
                                   @QueryParam("batch") @DefaultValue("false") boolean batch,
                                   @QueryParam("tag") String tag) {
        if (tag != null) {
            return tagLatestResource.getLatestCount(tag, count, stable, trace);
        }
        Optional<ContentKey> latest = channelService.getLatest(channel, stable, trace);
        if (!latest.isPresent()) {
            return Response.status(NOT_FOUND).build();
        }
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(latest.get())
                .next(false)
                .stable(stable)
                .ttlDays(channelService.getCachedChannelConfig(channel).getTtlDays())
                .count(count - 1)
                .build();
        query.trace(trace);
        Collection<ContentKey> keys = channelService.getKeys(query);
        keys.add(latest.get());
        String boundary = RandomStringUtils.randomAlphanumeric(70);
        if (batch) {
            //todo - gfm - 8/19/15 - factor this out
            StreamingOutput stream = new StreamingOutput() {
                @Override
                public void write(OutputStream os) throws IOException,
                        WebApplicationException {
                    URI channelUri = LinkBuilder.buildChannelUri(channel, uriInfo);
                    BufferedOutputStream output = new BufferedOutputStream(os);
                    for (ContentKey key : keys) {
                        Request request = Request.builder()
                                .channel(channel)
                                .key(key)
                                .build();
                        Optional<Content> content = channelService.getValue(request);
                        if (content.isPresent()) {
                            Content item = content.get();
                            output.write(("--" + boundary + CRLF).getBytes());
                            if (item.getContentType().isPresent()) {
                                output.write(("Content-Type: " + item.getContentType().get() + CRLF).getBytes());
                            }
                            URI uri = LinkBuilder.buildItemUri(key, channelUri);
                            output.write(("Content-Key: " + uri.toString() + CRLF).getBytes());
                            output.write(CRLF.getBytes());
                            ByteStreams.copy(item.getStream(), output);
                            output.write(CRLF.getBytes());
                            output.flush();
                        }
                    }
                    output.write(("--" + boundary + "--").getBytes());
                    output.flush();
                }
            };
            Response.ResponseBuilder builder = Response.ok(stream);
            builder.type("multipart/mixed; boundary=" + boundary);

            return builder.build();
        } else {
            return LinkBuilder.directionalResponse(channel, keys, count, query, mapper, uriInfo, true);
        }

    }

}
