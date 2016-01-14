package com.flightstats.hub.stream;

import com.flightstats.hub.model.Content;
import org.glassfish.jersey.server.ChunkedOutput;

import java.io.IOException;

public class ContentOutput extends ChunkedOutput<StreamContent> {

    private boolean first = true;
    private String channel;

    public ContentOutput(String channel) {
        this.channel = channel;
    }

    @Override
    public void write(StreamContent streamContent) throws IOException {
        if (streamContent.isFirst()) {
            first = false;
        }
        super.write(streamContent);
    }

    public void write(Content content) throws IOException {
        write(StreamContent.builder()
                .first(first)
                .content(content)
                .channel(channel).build());
    }

    public String getChannel() {
        return channel;
    }
}
