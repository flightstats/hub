package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Request;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Traces;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.SortedSet;
import java.util.function.Consumer;

public class MultiPartBatchBuilder {

    private final static Logger logger = LoggerFactory.getLogger(MultiPartBatchBuilder.class);

    private static final byte[] CRLF = "\r\n".getBytes();
    private static final String BOUNDARY = "||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||";
    private static final byte[] START_BOUNDARY = ("--" + BOUNDARY + "\r\n").getBytes();
    private static final byte[] END_BOUNDARY = ("--" + BOUNDARY + "--").getBytes();
    private static final String MULTIPART = "multipart/mixed; boundary=" + BOUNDARY;
    private static final byte[] CONTENT_TYPE = "Content-Type: ".getBytes();
    private static final byte[] CONTENT_KEY = "Content-Key: ".getBytes();

    public static Response build(SortedSet<ContentKey> keys, String channel,
                                 ChannelService channelService, UriInfo uriInfo) {
        Traces traces = ActiveTraces.getLocal();
        return write((BufferedOutputStream output) -> {
            ActiveTraces.setLocal(traces);
            for (ContentKey key : keys) {
                writeContent(uriInfo, output, key, channel, channelService);
            }
        });
    }

    public static Response buildTag(String tag, SortedSet<ChannelContentKey> keys,
                                    ChannelService channelService, UriInfo uriInfo) {
        Traces traces = ActiveTraces.getLocal();
        return write((BufferedOutputStream output) -> {
            ActiveTraces.setLocal(traces);
            for (ChannelContentKey key : keys) {
                writeContent(uriInfo, output, key.getContentKey(), key.getChannel(), channelService);
            }
        });
    }

    private static Response write(final Consumer<BufferedOutputStream> consumer) {
        Traces traces = ActiveTraces.getLocal();
        Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
            ActiveTraces.setLocal(traces);
            BufferedOutputStream output = new BufferedOutputStream(os);
            consumer.accept(output);
            output.write(END_BOUNDARY);
            output.flush();
        });
        builder.type(MULTIPART);
        return builder.build();
    }

    private static void writeContent(UriInfo uriInfo, BufferedOutputStream output, ContentKey key, String channel,
                                     ChannelService channelService) {
        try {
            Request request = Request.builder()
                    .channel(channel)
                    .key(key)
                    .build();
            Optional<Content> content = channelService.getValue(request);
            if (content.isPresent()) {
                URI channelUri = LinkBuilder.buildChannelUri(channel, uriInfo);
                Content item = content.get();
                output.write(START_BOUNDARY);
                if (item.getContentType().isPresent()) {
                    output.write(CONTENT_TYPE);
                    output.write(item.getContentType().get().getBytes());
                    output.write(CRLF);
                }
                URI uri = LinkBuilder.buildItemUri(key, channelUri);
                output.write(CONTENT_KEY);
                output.write(uri.toString().getBytes());
                output.write(CRLF);
                output.write(CRLF);
                ByteStreams.copy(item.getStream(), output);
                output.write(CRLF);
                output.flush();
            }
        } catch (IOException e) {
            logger.warn("io exception batching to " + channel, e);
            throw new RuntimeException(e);
        }
    }


}
