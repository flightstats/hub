package com.flightstats.hub.model;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class BulkContent {

    private final boolean isNew;
    private final InputStream stream;
    private final String contentType;
    private final String channel;
    private final List<Content> items;
    private ContentKey masterKey;

    @java.beans.ConstructorProperties({"isNew", "stream", "contentType", "channel", "masterKey", "items"})
    BulkContent(boolean isNew, InputStream stream, String contentType, String channel, ContentKey masterKey, List<Content> items) {
        this.isNew = isNew;
        this.stream = stream;
        this.contentType = contentType;
        this.channel = channel;
        this.masterKey = masterKey;

        if (items == null) {
            this.items = new ArrayList<>();
        } else {
            this.items = items;
        }
    }

    public static BulkContent fromMap(String channelName, Map<ContentKey, Content> map) {
        return BulkContent.builder()
                .channel(channelName)
                .items(new ArrayList<>(map.values()))
                .build();
    }

    public static BulkContentBuilder builder() {
        return new BulkContentBuilder();
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

    public void setMasterKey(ContentKey masterKey) {
        this.masterKey = masterKey;
    }

    public BulkContent withChannel(String channel) {
        return this.channel == channel ? this : new BulkContent(this.isNew, this.stream, this.contentType, channel, this.masterKey, this.items);
    }

    public static class BulkContentBuilder {
        private boolean isNew;
        private InputStream stream;
        private String contentType;
        private String channel;
        private ContentKey masterKey;
        private List<Content> items = new ArrayList<>();

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

        public BulkContent.BulkContentBuilder items(List<Content> items) {
            this.items = items;
            return this;
        }

        public BulkContent build() {
            return new BulkContent(isNew, stream, contentType, channel, masterKey, items);
        }

        public String toString() {
            return "com.flightstats.hub.model.BulkContent.BulkContentBuilder(isNew=" + this.isNew + ", stream=" + this.stream + ", contentType=" + this.contentType + ", channel=" + this.channel + ", masterKey=" + this.masterKey + ", items=" + this.items.size() + ")";
        }
    }
}
