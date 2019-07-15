package com.flightstats.hub.model;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.Delegate;
import okhttp3.HttpUrl;

@Builder
@Value
public class ChannelItemWithBody {
    @Delegate
    ChannelItemPathParts pathParts;

    Object body;

    public static class ChannelItemWithBodyBuilder {
        private String itemUrl;
        private HttpUrl baseUrl;

        public ChannelItemWithBodyBuilder baseUrl(HttpUrl baseUrl) {
            this.baseUrl = baseUrl;
            setPathParts();
            return this;
        }

        public ChannelItemWithBodyBuilder itemUrl(String itemUrl) {
            this.itemUrl = itemUrl;
            setPathParts();
            return this;
        }

        private void setPathParts() {
            pathParts = ChannelItemPathParts.builder()
                    .baseUrl(baseUrl)
                    .itemUrl(itemUrl)
                    .build();
        }
    }
}
