package com.flightstats.hub.model;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.util.HubUtils;
import com.google.common.io.ByteStreams;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Optional;

public class Content implements Serializable {
    public static final int THREADS = HubProperties.getProperty("s3.large.threads", 3);
    private final static Logger logger = LoggerFactory.getLogger(Content.class);
    private static final long serialVersionUID = 1L;
    private final Optional<String> contentType;
    //contentLength is the number of bytes in the total compressed payload (meta & item)
    private long contentLength;
    private InputStream stream;
    private byte[] data;
    private Optional<ContentKey> contentKey = Optional.empty();
    //size is the number of bytes in the raw, uncompressed item
    private Long size;
    private transient boolean isLarge;
    private transient int threads;
    private transient boolean isHistorical;
    private boolean replicated;

    private Content(Builder builder) {
        contentKey = builder.contentKey;
        contentType = builder.contentType;
        stream = builder.stream;
        contentLength = builder.contentLength;
        threads = Math.max(THREADS, builder.threads);
        isLarge = builder.large;
        size = builder.size;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Content copy(Content content) {
        Builder contentBuilder = Content.builder();

        if (content.getContentType().isPresent()) {
            contentBuilder.withContentType(content.getContentType().get());
        }

        if (content.getContentKey().isPresent()) {
            contentBuilder.withContentKey(content.getContentKey().get());
        }

        contentBuilder.withData(content.getData());
        contentBuilder.withContentLength(content.getContentLength());
        contentBuilder.withSize(content.getSize());
        contentBuilder.withLarge(content.isLarge());
        contentBuilder.withThreads(content.getThreads());

        return contentBuilder.build();
    }

    public boolean isNew() {
        return !contentKey.isPresent();
    }

    public boolean isIndexForLarge() {
        return getContentType().isPresent()
                && getContentType().get().equals(LargeContentUtils.CONTENT_TYPE);
    }

    public ContentKey keyAndStart(DateTime effectiveNow) {
        if (isNew()) {
            ContentKey key = new ContentKey(effectiveNow);
            setContentKey(key);
            ActiveTraces.getLocal().setStart(key.getMillis());
        } else {
            ActiveTraces.getLocal().setStart(System.currentTimeMillis());
        }
        return getContentKey().get();
    }

    public boolean isHistorical() {
        return this.isHistorical;
    }

    public void setHistorical(boolean isHistorical) {
        this.isHistorical = isHistorical;
    }

    public boolean isReplicated() {
        return replicated;
    }

    public void replicated() {
        replicated = true;
    }

    public InputStream getStream() {
        if (stream == null) {
            return new ByteArrayInputStream(getData());
        }
        return stream;
    }

    public void packageStream() throws IOException {
        if (isLarge || contentLength >= HubProperties.getLargePayload()) {
            isLarge = true;
        } else {
            data = ContentMarshaller.toBytes(this);
            stream = null;
        }
    }

    public byte[] getData() {
        if (data == null && stream != null) {
            try {
                data = ByteStreams.toByteArray(stream);
                stream = null;
            } catch (EOFException e) {
                logger.info("file ended early {}", contentKey);
            } catch (Exception e) {
                logger.warn("no data " + contentKey, e);
            }
        }
        return data;
    }

    public Long getSize() {
        if (size == null) {
            if (data == null) {
                size = -1L;
            } else {
                size = (long) data.length;
            }
        }
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public void close() {
        HubUtils.closeQuietly(stream);
    }

    public Optional<String> getContentType() {
        return this.contentType;
    }

    public long getContentLength() {
        return this.contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public Optional<ContentKey> getContentKey() {
        return this.contentKey;
    }

    public void setContentKey(ContentKey contentKey) {
        this.contentKey = Optional.of(contentKey);
    }

    public boolean isLarge() {
        return this.isLarge;
    }

    public int getThreads() {
        return this.threads;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Content)) return false;
        final Content other = (Content) o;
        if (!other.canEqual(this)) return false;
        final Object this$contentType = this.getContentType();
        final Object other$contentType = other.getContentType();
        return this$contentType == null ? other$contentType == null : this$contentType.equals(other$contentType);
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $contentType = this.getContentType();
        result = result * PRIME + ($contentType == null ? 43 : $contentType.hashCode());
        return result;
    }

    protected boolean canEqual(Object other) {
        return other instanceof Content;
    }

    public static class Builder {
        private Optional<String> contentType = Optional.empty();
        private long contentLength = 0;
        private Long size;
        private Optional<ContentKey> contentKey = Optional.empty();
        private InputStream stream;
        private int threads;
        private boolean large;

        public Builder withContentType(String contentType) {
            this.contentType = Optional.ofNullable(contentType);
            return this;
        }

        public Builder withContentKey(ContentKey contentKey) {
            this.contentKey = Optional.ofNullable(contentKey);
            return this;
        }

        public Builder withStream(InputStream stream) {
            this.stream = stream;
            return this;
        }

        public Builder withContentLength(Long contentLength) {
            this.contentLength = contentLength;
            return this;
        }

        public Builder withSize(Long size) {
            this.size = size;
            return this;
        }

        public Builder withData(byte[] data) {
            this.stream = new ByteArrayInputStream(data);
            return this;
        }

        public Content build() {
            return new Content(this);
        }

        public Builder withThreads(int threads) {
            this.threads = threads;
            return this;
        }

        public Builder withLarge(boolean large) {
            this.large = large;
            return this;
        }

        public Optional<String> getContentType() {
            return this.contentType;
        }

        public long getContentLength() {
            return this.contentLength;
        }

        public long getSize() {
            return this.size;
        }

        public Optional<ContentKey> getContentKey() {
            return this.contentKey;
        }

        public InputStream getStream() {
            return this.stream;
        }

        public int getThreads() {
            return this.threads;
        }
    }
}
