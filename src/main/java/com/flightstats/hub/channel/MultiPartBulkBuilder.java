package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ItemRequest;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.StreamResults;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.io.ByteStreams;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;
import java.util.SortedSet;
import java.util.function.Consumer;

@Slf4j
class MultiPartBulkBuilder {

    private static final byte[] CRLF = "\r\n".getBytes();
    private static final String BOUNDARY = "||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||~~~~~~||||||";
    private static final byte[] START_BOUNDARY = ("--" + BOUNDARY + "\r\n").getBytes();
    private static final byte[] END_BOUNDARY = ("--" + BOUNDARY + "--").getBytes();
    private static final String MULTIPART = "multipart/mixed; boundary=" + BOUNDARY;
    private static final byte[] CONTENT_TYPE = "Content-Type: ".getBytes();
    private static final byte[] CONTENT_KEY = "Content-Key: ".getBytes();
    private static final byte[] CREATION_DATE = "Creation-Date: ".getBytes();

    private final LinkBuilder linkBuilder;

    @Inject
    public MultiPartBulkBuilder(LinkBuilder linkBuilder) {
        this.linkBuilder = linkBuilder;
    }

    public Response build(SortedSet<ContentKey> keys,
                          String channel,
                          ChannelService channelService,
                          UriInfo uriInfo,
                          Consumer<Response.ResponseBuilder> headerBuilder,
                          boolean descending) {
        Traces traces = ActiveTraces.getLocal();
        return write((BufferedOutputStream output) -> {
            ActiveTraces.setLocal(traces);
            channelService.get(StreamResults.builder()
                    .channel(channel)
                    .keys(keys)
                    .callback(content -> writeContent(content, output,
                            this.linkBuilder.buildChannelUri(channel, uriInfo), channel))
                    .descending(descending)
                    .build());
        }, headerBuilder);
    }

    Response buildTag(SortedSet<ChannelContentKey> keys,
                      ChannelService channelService,
                      UriInfo uriInfo,
                      Consumer<Response.ResponseBuilder> headerBuilder) {
        Traces traces = ActiveTraces.getLocal();
        return write((BufferedOutputStream output) -> {
            ActiveTraces.setLocal(traces);
            for (ChannelContentKey key : keys) {
                writeContent(uriInfo, output, key.getContentKey(), key.getChannel(), channelService);
            }
        }, headerBuilder);
    }

    private Response write(Consumer<BufferedOutputStream> consumer,
                           Consumer<Response.ResponseBuilder> headerBuilder) {
        Traces traces = ActiveTraces.getLocal();
        Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
            ActiveTraces.setLocal(traces);
            BufferedOutputStream output = new BufferedOutputStream(os);
            consumer.accept(output);
            output.write(END_BOUNDARY);
            output.flush();
        });
        builder.type(MULTIPART);
        headerBuilder.accept(builder);
        return builder.build();
    }

    private void writeContent(UriInfo uriInfo,
                              BufferedOutputStream output,
                              ContentKey key,
                              String channel,
                              ChannelService channelService) {
        ItemRequest itemRequest = ItemRequest.builder()
                .channel(channel)
                .key(key)
                .build();
        Optional<Content> content = channelService.get(itemRequest);
        if (content.isPresent()) {
            Content item = content.get();
            writeContent(item, output, this.linkBuilder.buildChannelUri(channel, uriInfo), channel);
        }
    }

    private void writeContent(Content content, OutputStream output, URI channelUri, String name) {
        writeContent(content, output, channelUri, name, true, false);
    }

    private void writeContent(Content content,
                              OutputStream output,
                              URI channelUri,
                              String name,
                              boolean startBoundary,
                              boolean endBoundary) {
        try {
            if (startBoundary) output.write(START_BOUNDARY);
            if (content.getContentType().isPresent()) {
                output.write(CONTENT_TYPE);
                output.write(content.getContentType().get().getBytes());
                output.write(CRLF);
            }
            URI uri = this.linkBuilder.buildItemUri(content.getContentKey().get(), channelUri);
            output.write(CONTENT_KEY);
            output.write(uri.toString().getBytes());
            output.write(CRLF);
            output.write(CREATION_DATE);
            output.write(TimeUtil.FORMATTER.print(content.getContentKey().get().getMillis()).getBytes());
            output.write(CRLF);
            output.write(CRLF);
            ByteStreams.copy(content.getStream(), output);
            output.write(CRLF);
            if (endBoundary) output.write(START_BOUNDARY);
            output.flush();

        } catch (IOException e) {
            log.warn("io exception batching to " + name, e);
            throw new RuntimeException(e);
        }
    }

}
