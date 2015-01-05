package com.flightstats.hub.replication;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.flightstats.hub.model.exception.InvalidRequestException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

@EqualsAndHashCode
@ToString
@Builder
@Getter
public class ReplicationDomain {
    private final static Logger logger = LoggerFactory.getLogger(ReplicationDomain.class);

    private String domain;
    private final long historicalDays;
    private final SortedSet<String> excludeExcept;

    @JsonIgnore()
    public boolean isValid() {
        if (null == excludeExcept) {
            return false;
        }
        return !excludeExcept.isEmpty();
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @JsonCreator
    protected static ReplicationDomain create(Map<String, JsonNode> props) {
        ReplicationDomainBuilder builder = builder();
        for (Map.Entry<String, JsonNode> entry : props.entrySet()) {
            switch (entry.getKey()) {
                case "historicalDays":
                    long historicalDaysValue = entry.getValue().asLong(-1);
                    if (historicalDaysValue < 0) {
                        throw new InvalidRequestException("historicalDays must be a number, zero or greater.");
                    }
                    builder.historicalDays(historicalDaysValue);
                    break;
                case "excludeExcept":
                    builder.excludeExcept(convert(entry.getValue()));
                    break;
                case "includeExcept":
                    throw new InvalidRequestException("includeExcept is no longer supported");
                default:
                    logger.info("unexpected key " + entry.getKey() + " " + entry.getValue());
                    break;
            }
        }
        return builder.build();
    }

    private static SortedSet<String> convert(JsonNode node) {
        SortedSet<String> values = new TreeSet<>();
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

}
