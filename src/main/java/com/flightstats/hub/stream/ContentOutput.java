package com.flightstats.hub.stream;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.channel.LinkBuilder;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import lombok.Getter;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

import javax.ws.rs.core.UriBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

@Getter
public class ContentOutput implements Closeable {

    private static final URI APP_URL = UriBuilder.fromPath(HubProperties.getAppUrl()).build();

    private final String channel;
    private final EventOutput eventOutput;
    private final ContentKey contentKey;
    private final URI channelUri;

    public ContentOutput(String channel, EventOutput eventOutput, ContentKey contentKey) {
        this.channel = channel;
        this.eventOutput = eventOutput;
        this.contentKey = contentKey;
        channelUri = UriBuilder.fromUri(APP_URL).path("channel/" + channel).build();
    }

    public void writeHeartbeat() throws IOException {
        eventOutput.write(new OutboundEvent.Builder().comment("").build());
    }

    public void write(Content content) throws IOException {
        URI uri = LinkBuilder.buildItemUri(content.getContentKey().get(), channelUri);
        OutboundEvent.Builder builder = new OutboundEvent.Builder().id(uri.toString());
        if (content.getContentType().isPresent()) {
            builder.name(content.getContentType().get());
        }
        eventOutput.write(builder.data(byte[].class, content.getData()).build());
    }

    @Override
    public void close() throws IOException {
        IOUtils.closeQuietly(eventOutput);
    }
}
