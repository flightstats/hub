package com.flightstats.datahub.model;

import java.net.URI;

public class ChannelCreationResponse {

    private final Links links;
    private final ChannelConfiguration channelConfiguration;

    public ChannelCreationResponse(URI channelUri, URI latestUri, ChannelConfiguration channelConfiguration) {
        this(new Links(channelUri, latestUri), channelConfiguration);
    }

    public ChannelCreationResponse(Links links, ChannelConfiguration channelConfiguration){
        this.links = links;
        this.channelConfiguration = channelConfiguration;
    }

    public URI getChannelUri() {
        return links.getChannelUri();
    }

    public ChannelConfiguration getChannelConfiguration() {
        return channelConfiguration;
    }

    public static class Links {
        private final URI channelUri;
        private final URI latestUri;

        public Links(URI latestUri, URI channelUri) {
            this.channelUri = channelUri;
            this.latestUri = latestUri;
        }

        public URI getChannelUri() {
            return channelUri;
        }

        public URI getLatestUri() {
            return latestUri;
        }
    }

}
