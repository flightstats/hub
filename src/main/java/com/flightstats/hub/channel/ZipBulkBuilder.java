package com.flightstats.hub.channel;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.dao.ItemRequest;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.StreamResults;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipBulkBuilder {

    private final static Logger logger = LoggerFactory.getLogger(ZipBulkBuilder.class);

    public static Response build(SortedSet<ContentKey> keys, String channel,
                                 ChannelService channelService, boolean descending, Consumer<Response.ResponseBuilder> headerBuilder) {
        return write(getConsumer(keys, channel, channelService, descending, ActiveTraces.getLocal(), false), headerBuilder);
    }

    public static byte[] build(SortedSet<ContentKey> keys, String channel, ChannelService channelService, boolean descending, boolean skipLarge) {
        return write(getConsumer(keys, channel, channelService, descending, ActiveTraces.getLocal(), skipLarge));
    }

    private static Consumer<ZipOutputStream> getConsumer(SortedSet<ContentKey> keys, String channel,
                                                         ChannelService channelService, boolean descending, Traces traces, boolean skipLarge) {
        return (ZipOutputStream output) -> {
            ActiveTraces.setLocal(traces);
            channelService.get(StreamResults.builder()
                    .channel(channel)
                    .keys(keys)
                    .callback(content -> createZipEntry(output, content))
                    .descending(descending)
                    .skipLarge(skipLarge)
                    .build()
            );
        };
    }

    static Response buildTag(String tag, SortedSet<ChannelContentKey> keys,
                             ChannelService channelService, Consumer<Response.ResponseBuilder> headerBuilder) {
        Traces traces = ActiveTraces.getLocal();
        return write((ZipOutputStream output) -> {
            ActiveTraces.setLocal(traces);
            for (ChannelContentKey key : keys) {
                writeContent(output, key.getContentKey(), key.getChannel(), channelService);
            }
        }, headerBuilder);
    }

    private static byte[] write(Consumer<ZipOutputStream> consumer) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        handleOutput(consumer, os);
        return os.toByteArray();
    }

    private static Response write(Consumer<ZipOutputStream> consumer, Consumer<Response.ResponseBuilder> headerBuilder) {
        Traces traces = ActiveTraces.getLocal();
        Response.ResponseBuilder builder = Response.ok((StreamingOutput) os -> {
            ActiveTraces.setLocal(traces);
            handleOutput(consumer, os);
        });
        builder.type("application/zip");
        headerBuilder.accept(builder);
        return builder.build();
    }

    private static void handleOutput(Consumer<ZipOutputStream> consumer, OutputStream os) {
        try {
            ZipOutputStream output = new ZipOutputStream(os);
            output.setLevel(Deflater.DEFAULT_COMPRESSION);
            consumer.accept(output);
            output.flush();
            output.close();
        } catch (Exception e) {
            logger.warn("issue with streaming bulk", e);
            throw new RuntimeException(e);
        }
    }

    private static void writeContent(ZipOutputStream output, ContentKey key, String channel,
                                     ChannelService channelService) {
        ItemRequest itemRequest = ItemRequest.builder()
                .channel(channel)
                .key(key)
                .build();
        Optional<Content> contentOptional = channelService.get(itemRequest);
        if (contentOptional.isPresent()) {
            createZipEntry(output, contentOptional.get());
        } else {
            logger.warn("missing content for zip {} {}", channel, key);
        }
    }

    public static void createZipEntry(ZipOutputStream output, Content content) {
        try {
            String keyId = content.getContentKey().get().toUrl();
            ZipEntry zipEntry = new ZipEntry(keyId);
            zipEntry.setExtra(ContentMarshaller.getMetaData(content).getBytes());
            output.putNextEntry(zipEntry);
            long bytesCopied = ByteStreams.copy(content.getStream(), output);
            zipEntry.setSize(bytesCopied);
        } catch (IOException e) {
            logger.warn("exception zip batching for  " + content.getContentKey().get(), e);
            throw new RuntimeException(e);
        }
    }


}
