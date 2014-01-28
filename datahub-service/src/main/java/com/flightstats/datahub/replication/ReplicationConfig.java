package com.flightstats.datahub.replication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class ReplicationConfig {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationConfig.class);

    private String domain;
    private final long historicalDays;
    private final Set<String> includeExcept;
    private final Set<String> excludeExcept;

    public ReplicationConfig(Builder builder) {
        domain = builder.domain;
        historicalDays = builder.historicalDays;
        includeExcept = Collections.unmodifiableSet(new TreeSet<>(builder.includedExcept));
        excludeExcept = Collections.unmodifiableSet(new TreeSet<>(builder.excludedExcept));
    }

    @JsonCreator
    protected static ReplicationConfig create(Map<String, JsonNode> props) {
        Builder builder = builder();
        for (Map.Entry<String, JsonNode> entry : props.entrySet()) {
            switch (entry.getKey()) {
                case "historicalDays":
                    builder.withHistoricalDays(entry.getValue().asLong());
                    break;
                case "excludeExcept":
                    builder.withExcludedExcept(convert(entry.getValue()));
                    break;
                case "includeExcept":
                    builder.withIncludedExcept(convert(entry.getValue()));
                    break;
                default:
                    logger.info("unexpected key " + entry.getKey() + " " + entry.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private static Set<String> convert(JsonNode node) {
        Set<String> values = new HashSet<>();
        if (node.isArray()) {
            ArrayNode array = (ArrayNode) node;
            for (JsonNode item : array) {
                values.add(item.asText());
            }
        } else {
            values.add(node.asText());
        }
        return values;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @JsonProperty("historicalDays")
    public long getHistoricalDays() {
        return historicalDays;
    }

    @JsonProperty("includeExcept")
    public Set<String> getIncludeExcept() {
        return includeExcept;
    }

    @JsonProperty("excludeExcept")
    public Set<String> getExcludeExcept() {
        return excludeExcept;
    }

    @Override
    public String toString() {
        return "ReplicationConfig{" +
                "domain='" + domain + '\'' +
                ", historicalDays=" + historicalDays +
                ", includeExcept=" + includeExcept +
                ", excludeExcept=" + excludeExcept +
                '}';
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String domain;
        private long historicalDays = 0;
        private Set<String> includedExcept = new HashSet<>();
        private Set<String> excludedExcept = new HashSet<>();

        public Builder withHistoricalDays(long historicalDays) {
            this.historicalDays = historicalDays;
            return this;
        }

        public Builder withIncludedExcept(Collection<String> included) {
            this.includedExcept.addAll(included);
            return this;
        }

        public Builder withExcludedExcept(Collection<String> excluded) {
            this.excludedExcept.addAll(excluded);
            return this;
        }

        public Builder withDomain(String domain) {
            this.domain = domain;
            return this;
        }

        public ReplicationConfig build() {
            return new ReplicationConfig(this);
        }
    }
}
