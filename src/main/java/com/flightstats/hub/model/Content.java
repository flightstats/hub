package com.flightstats.hub.model;

import com.flightstats.hub.metrics.ActiveTraces;
import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.io.Serializable;

@Getter
@EqualsAndHashCode(of = {"contentType", "contentLanguage"})
public class Content implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(Content.class);

    private static final long serialVersionUID = 1L;

    private final Optional<String> contentType;
    private final Optional<String> contentLanguage;
    private final boolean isNew;
    private final InputStream stream;
    private byte[] data;
    private Optional<ContentKey> contentKey = Optional.absent();
    @Setter
    private Long size;

    private Content(Builder builder) {
        contentKey = builder.contentKey;
        isNew = !getContentKey().isPresent();
        contentLanguage = builder.contentLanguage;
        contentType = builder.contentType;
        stream = builder.stream;
    }

    public static Builder builder() {
        return new Builder();
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

    public byte[] getData() {
        if (data == null && stream != null) {
            try {
                data = ByteStreams.toByteArray(stream);
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
            size = new Long(data.length);
        }
        return size;
    }

    public static class Builder {
        private Optional<String> contentType = Optional.absent();
        private Optional<String> contentLanguage = Optional.absent();
        public Optional<ContentKey> contentKey = Optional.absent();
        private InputStream stream;

        public Builder withContentType(String contentType) {
            this.contentType = Optional.fromNullable(contentType);
            return this;
        }

        public Builder withContentLanguage(String contentLanguage) {
            this.contentLanguage = Optional.fromNullable(contentLanguage);
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

        public Builder withData(byte[] data) {
            this.stream = new ByteArrayInputStream(data);
            return this;
        }

        public Content build() {
            return new Content(this);
        }

    }
}
