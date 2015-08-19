package com.flightstats.hub.model;

import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BatchContent {

    private final boolean isNew = true;
    private final InputStream stream;
    private final String contentType;
    private final List<Content> items = new ArrayList<>();

    private Traces traces = new TracesImpl();

    private BatchContent(Builder builder) {
        stream = builder.stream;
        contentType = builder.contentType;
    }

    public long getSize() {
        long bytes = 0;
        for (Content item : items) {
            bytes += item.getSize();
        }
        return bytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String contentType;
        private InputStream stream;

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withStream(InputStream stream) {
            this.stream = stream;
            return this;
        }

        public Builder withData(byte[] data) {
            this.stream = new ByteArrayInputStream(data);
            return this;
        }

        public BatchContent build() {
            return new BatchContent(this);
        }

    }
}
