package com.flightstats.hub.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@EqualsAndHashCode(of = {"data", "contentType", "contentLanguage", "user"})
public class Content2 implements Serializable {
    private final static Logger logger = LoggerFactory.getLogger(Content2.class);

    private static final long serialVersionUID = 1L;

    private final String contentType;
    private final String contentLanguage;
    private final byte[] data;
    private final String user;
    private final boolean isNew;
    private ContentKey contentKey;
    private List<Trace> traces = Collections.synchronizedList(new ArrayList<>());

    private Content2(Builder builder) {
        contentKey = builder.contentKey;
        isNew = contentKey != null;
        contentLanguage = builder.contentLanguage;
        contentType = builder.contentType;
        data = builder.data;
        user = builder.user;
        traces.add(new Trace("Content.start"));
    }

    public static Builder builder() {
        return new Builder();
    }

    public void setContentKey(ContentKey contentKey) {
        this.contentKey = contentKey;
    }

/*
    public void logTraces() {
        long processingTime = System.currentTimeMillis() - contentKey.getMillis();
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
*/

    /**
     * @return true if this Content is new, false if it has been inserted elsewhere and is a replicant.
     */
    public boolean isNewContent() {
        return isNew;
    }

    public static class Builder {
        private String contentType;
        private String contentLanguage;
        private ContentKey contentKey;
        private byte[] data;
        private String user;

        public Builder withData(byte[] data) {
            this.data = data;
            return this;
        }

        public Builder withContentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder withContentLanguage(String contentLanguage) {
            this.contentLanguage = contentLanguage;
            return this;
        }

        public Builder withContentKey(ContentKey contentKey) {
            this.contentKey = contentKey;
            return this;
        }

        public Builder withUser(String user) {
            this.user = user;
            return this;
        }

        public Content2 build() {
            return new Content2(this);
        }

    }
}
