package com.flightstats.hub.model;

import com.google.common.base.Optional;
import com.google.common.io.ByteStreams;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;

@Getter
@EqualsAndHashCode(of = {"contentType", "contentLanguage", "user"})
public class Content implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(Content.class);

    private static final long serialVersionUID = 1L;

    private final Optional<String> contentType;
    private final Optional<String> contentLanguage;
    private final Optional<String> user;
    private final boolean isNew;
    private final InputStream stream;
    private byte[] data;
    private Optional<ContentKey> contentKey = Optional.absent();
    private Traces traces = new TracesImpl();

    private Content(Builder builder) {
        contentKey = builder.contentKey;
        isNew = !getContentKey().isPresent();
        contentLanguage = builder.contentLanguage;
        contentType = builder.contentType;
        user = builder.user;
        stream = builder.stream;
        traces.add(new Trace("Content.start"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public ContentKey keyAndStart() {
        if (isNew()) {
            ContentKey key = new ContentKey();
            setContentKey(key);
            getTraces().setStart(key.getMillis());
        } else {
            getTraces().setStart(System.currentTimeMillis());
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
            } catch (Exception e) {
                logger.warn("no data", e);
            }
        }
        return data;
    }

    public static class Builder {
        private Optional<String> contentType = Optional.absent();
        private Optional<String> contentLanguage = Optional.absent();
        private Optional<ContentKey> contentKey = Optional.absent();
        private Optional<String> user = Optional.absent();
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

        public Builder withUser(String user) {
            this.user = Optional.fromNullable(user);
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
