package com.flightstats.hub.replication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.model.exception.InvalidRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 *
 */
public class ReplicationDomain {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationDomain.class);

    private String domain;
    private final long historicalDays;
    private final Set<String> includeExcept;
    private final Set<String> excludeExcept;

    private ReplicationDomain(Builder builder) {
        domain = builder.domain;
        historicalDays = builder.historicalDays;
        includeExcept = Collections.unmodifiableSet(new TreeSet<>(builder.includedExcept));
        excludeExcept = Collections.unmodifiableSet(new TreeSet<>(builder.excludedExcept));
    }

    @JsonIgnore()
    public boolean isValid() {
        if (includeExcept.isEmpty()) {
            return !excludeExcept.isEmpty();
        }
        return excludeExcept.isEmpty() && !includeExcept.isEmpty();
    }

    @JsonIgnore()
    public boolean isInclusive() {
        return !includeExcept.isEmpty();
    }
    @JsonCreator
    protected static ReplicationDomain create(Map<String, JsonNode> props) {
        Builder builder = builder();
        for (Map.Entry<String, JsonNode> entry : props.entrySet()) {
            switch (entry.getKey()) {
                case "historicalDays":
                    long historicalDaysValue = entry.getValue().asLong();
                    if (historicalDaysValue < 0) {
                        throw new InvalidRequestException("historical days must be zero or greater.");
                    }
                    builder.withHistoricalDays(historicalDaysValue);
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

    public long getHistoricalDays() {
        return historicalDays;
    }

    public Set<String> getIncludeExcept() {
        return includeExcept;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReplicationDomain that = (ReplicationDomain) o;

        if (historicalDays != that.historicalDays) return false;
        if (!domain.equals(that.domain)) return false;
        if (!excludeExcept.equals(that.excludeExcept)) return false;
        if (!includeExcept.equals(that.includeExcept)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = domain.hashCode();
        result = 31 * result + (int) (historicalDays ^ (historicalDays >>> 32));
        result = 31 * result + includeExcept.hashCode();
        result = 31 * result + excludeExcept.hashCode();
        return result;
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

        public ReplicationDomain build() {
            return new ReplicationDomain(this);
        }
    }
}
