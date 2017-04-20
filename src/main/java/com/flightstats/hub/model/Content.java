package com.flightstats.hub.model;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.metrics.ActiveTraces;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.IOUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class Content implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(Content.class);

    private static final long serialVersionUID = 1L;

    private final Optional<String> contentType;
    //contentLength is the total compressed length of everything (meta & item)
    private long contentLength;
    //payloadLength is the size of the raw, uncompressed item
    private long payloadLength;
    private InputStream stream;
    private byte[] data;
    private Optional<ContentKey> contentKey = Optional.absent();
    //todo - gfm - is size different than payloadLength?
    private Long size;
    private transient boolean isLarge;
    private transient int threads;

    private Content(Builder builder) {
        contentKey = builder.contentKey;
        contentType = builder.contentType;
        stream = builder.stream;
        contentLength = builder.contentLength;
        threads = builder.threads;
        payloadLength = builder.payloadLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isNew() {
        return !contentKey.isPresent();
    }

    public boolean isIndexForLarge() {
        return getContentType().isPresent()
                && getContentType().get().equals(LargeContent.CONTENT_TYPE);
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

    public void setContentKey(ContentKey contentKey) {
        this.contentKey = Optional.of(contentKey);
    }

    public InputStream getStream() {
        if (stream == null) {
            return new ByteArrayInputStream(getData());
        }
        return stream;
    }

    public void packageStream() throws IOException {
        if (contentLength < HubProperties.getLargePayload()) {
            data = ContentMarshaller.toBytes(this);
            stream = null;
        } else {
            isLarge = true;
        }
    }

    public byte[] getData() {
        if (data == null && stream != null) {
            //todo gfm - can this go away?
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
                throw new UnsupportedOperationException("convert stream to bytes first");
            }
            size = (long) data.length;
        }
        return size;
    }

    public void close() {
        IOUtils.closeQuietly(stream);
    }

    public Optional<String> getContentType() {
        return this.contentType;
    }

    public long getContentLength() {
        return this.contentLength;
    }

    public long getPayloadLength() {
        return this.payloadLength;
    }

    public Optional<ContentKey> getContentKey() {
        return this.contentKey;
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
        if (!other.canEqual((Object) this)) return false;
        final Object this$contentType = this.getContentType();
        final Object other$contentType = other.getContentType();
        if (this$contentType == null ? other$contentType != null : !this$contentType.equals(other$contentType))
            return false;
        return true;
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

    public void setSize(Long size) {
        this.size = size;
    }

    //todo - gfm - would be nice with more lombok
    public static class Builder {
        private Optional<String> contentType = Optional.absent();
        private long contentLength = 0;
        private long payloadLength = -1;
        private Optional<ContentKey> contentKey = Optional.absent();
        private InputStream stream;
        private int threads;

        public Builder withContentType(String contentType) {
            this.contentType = Optional.fromNullable(contentType);
            return this;
        }

        public Builder withContentKey(ContentKey contentKey) {
            this.contentKey = Optional.fromNullable(contentKey);
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

        public Builder withPayloadLength(Long payloadLength) {
            this.payloadLength = payloadLength;
            return this;
        }

        public Builder withData(byte[] data) {
            this.stream = new ByteArrayInputStream(data);
            return this;
        }

        public Content build() {
            return new Content(this);
        }

        public Builder withThreads(String threads) {
            this.threads = Integer.parseInt(threads);
            return this;
        }

        public Optional<String> getContentType() {
            return this.contentType;
        }

        public long getContentLength() {
            return this.contentLength;
        }

        public long getPayloadLength() {
            return this.payloadLength;
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
