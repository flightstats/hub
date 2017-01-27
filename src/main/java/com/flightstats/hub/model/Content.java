package com.flightstats.hub.model;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.metrics.ActiveTraces;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

@Getter
@EqualsAndHashCode(of = {"contentType"})
public class Content implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(Content.class);

    private static final long serialVersionUID = 1L;

    private final Optional<String> contentType;
    private long contentLength;
    private InputStream stream;
    private byte[] data;
    private Optional<ContentKey> contentKey = Optional.absent();
    @Setter
    private Long size;

    private Content(Builder builder) {
        contentKey = builder.contentKey;
        contentType = builder.contentType;
        stream = builder.stream;
        contentLength = builder.contentLength;
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isNew() {
        return !contentKey.isPresent();
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

    //todo - gfm - would be nice with more lombok
    @Getter
    public static class Builder {
        private Optional<String> contentType = Optional.absent();
        private long contentLength = 0;
        private Optional<ContentKey> contentKey = Optional.absent();
        private InputStream stream;

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

        public Builder withData(byte[] data) {
            this.stream = new ByteArrayInputStream(data);
            return this;
        }

        public Content build() {
            return new Content(this);
        }

    }
}
