package com.flightstats.hub.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BulkContent {

    private final boolean isNew;
    private final InputStream stream;
    private final String contentType;
    private final String channel;
    private final List<Content> items = new ArrayList<>();
    private ContentKey masterKey;

    @java.beans.ConstructorProperties({"isNew", "stream", "contentType", "channel", "masterKey"})
    BulkContent(boolean isNew, InputStream stream, String contentType, String channel, ContentKey masterKey) {
        this.isNew = isNew;
        this.stream = stream;
        this.contentType = contentType;
        this.channel = channel;
        this.masterKey = masterKey;
    }

    public static BulkContentBuilder builder() {
        return new BulkContentBuilder();
    }

    public void setMasterKey(ContentKey masterKey) {
        this.masterKey = masterKey;
    }

    public long getSize() {
        long bytes = 0;
        for (Content item : items) {
            bytes += item.getSize();
        }
        return bytes;
    }


    public boolean isNew() {
        return this.isNew;
    }

    public InputStream getStream() {
        return this.stream;
    }

    public String getContentType() {
        return this.contentType;
    }

    public String getChannel() {
        return this.channel;
    }

    public List<Content> getItems() {
        return this.items;
    }

    public ContentKey getMasterKey() {
        return this.masterKey;
    }

    public static class BulkContentBuilder {
        private boolean isNew;
        private InputStream stream;
        private String contentType;
        private String channel;
        private ContentKey masterKey;

        BulkContentBuilder() {
        }

        public BulkContent.BulkContentBuilder isNew(boolean isNew) {
            this.isNew = isNew;
            return this;
        }

        public BulkContent.BulkContentBuilder stream(InputStream stream) {
            this.stream = stream;
            return this;
        }

        public BulkContent.BulkContentBuilder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public BulkContent.BulkContentBuilder channel(String channel) {
            this.channel = channel;
            return this;
        }

        public BulkContent.BulkContentBuilder masterKey(ContentKey masterKey) {
            this.masterKey = masterKey;
            return this;
        }

        public BulkContent build() {
            return new BulkContent(isNew, stream, contentType, channel, masterKey);
        }

        public String toString() {
            return "com.flightstats.hub.model.BulkContent.BulkContentBuilder(isNew=" + this.isNew + ", stream=" + this.stream + ", contentType=" + this.contentType + ", channel=" + this.channel + ", masterKey=" + this.masterKey + ")";
        }
    }
}
