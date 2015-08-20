package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.RandomStringUtils;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;

public class MultiPartBuilder {

    public static final String CRLF = "\r\n";

    public static Response build(Collection<ContentKey> keys, String channel,
                                 ChannelService channelService, UriInfo uriInfo) {
        String boundary = RandomStringUtils.randomAlphanumeric(70);
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
    }
}
