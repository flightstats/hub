package com.flightstats.hub.events;

import com.flightstats.hub.channel.LinkBuilder;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.jersey.media.sse.OutboundEvent;

import javax.ws.rs.core.UriBuilder;
import java.io.Closeable;
import java.io.IOException;
import java.net.URI;

public class ContentOutput implements Closeable {

    private final String channel;
    private final EventOutput eventOutput;
    private final ContentKey contentKey;
    private final URI channelUri;

    public ContentOutput(String channel, EventOutput eventOutput, ContentKey contentKey, URI baseUri) {
        this.channel = channel;
        this.eventOutput = eventOutput;
        this.contentKey = contentKey;
        channelUri = UriBuilder.fromUri(baseUri).path("channel/" + channel).build();
    }

    void writeHeartbeat() throws IOException {
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

    public String getChannel() {
        return this.channel;
    }

    public EventOutput getEventOutput() {
        return this.eventOutput;
    }

    public ContentKey getContentKey() {
        return this.contentKey;
    }

    public URI getChannelUri() {
        return this.channelUri;
    }
}
