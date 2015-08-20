package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.function.BiConsumer;

public class MultiPartBuilder {

    public static final String CRLF = "\r\n";
    private final static Logger logger = LoggerFactory.getLogger(MultiPartBuilder.class);

    public static Response build(Collection<ContentKey> keys, String channel,
                                 ChannelService channelService, UriInfo uriInfo) {
        return write((BufferedOutputStream output, String boundary) -> {
            for (ContentKey key : keys) {
                writeContent(uriInfo, output, key, channel, channelService, boundary);
            }
        });
    }

    public static Response buildTag(String tag, Collection<ChannelContentKey> keys,
                                    ChannelService channelService, UriInfo uriInfo) {
        return write((BufferedOutputStream output, String boundary) -> {
            for (ChannelContentKey key : keys) {
                writeContent(uriInfo, output, key.getContentKey(), key.getChannel(), channelService, boundary);
            }
        });
    }

    private static Response write(final BiConsumer<BufferedOutputStream, String> consumer) {
        String boundary = RandomStringUtils.randomAlphanumeric(70);
        Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
            BufferedOutputStream output = new BufferedOutputStream(os);
            consumer.accept(output, boundary);
            output.write(("--" + boundary + "--").getBytes());
            output.flush();
        });
        builder.type("multipart/mixed; boundary=" + boundary);
        return builder.build();
    }

    private static void writeContent(UriInfo uriInfo, BufferedOutputStream output, ContentKey key, String channel,
                                     ChannelService channelService, String boundary) {
        try {
            Request request = Request.builder()
                    .channel(channel)
                    .key(key)
                    .build();
            Optional<Content> content = channelService.getValue(request);
            if (content.isPresent()) {
                URI channelUri = LinkBuilder.buildChannelUri(channel, uriInfo);
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
        } catch (IOException e) {
            logger.warn("io exception batching to " + channel, e);
            throw new RuntimeException(e);
        }
    }


}
