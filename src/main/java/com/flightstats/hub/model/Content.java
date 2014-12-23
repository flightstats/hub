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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    //todo - gfm - 12/22/14 - pull this out as it's own class
    private List<Trace> traces = Collections.synchronizedList(new ArrayList<>());

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

    public void setContentKey(ContentKey contentKey) {
        this.contentKey = Optional.of(contentKey);
    }

    public void logTraces() {
        long processingTime = System.currentTimeMillis() - contentKey.get().getMillis();
        if (processingTime >= 100) {
            try {
                traces.add(new Trace("logging"));
                String output = "\n\t";
                for (Trace trace : traces) {
                    output += trace.toString() + "\n\t";
                }
                logger.info("slow processing of {} millis. trace: {}", processingTime, output);
            } catch (Exception e) {
                logger.warn("unable to log {} traces {}", contentKey, traces);
            }
        }
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
    /**
     * @return true if this Content is new, false if it has been inserted elsewhere and is a replicant.
     */
    public boolean isNewContent() {
        return isNew;
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
