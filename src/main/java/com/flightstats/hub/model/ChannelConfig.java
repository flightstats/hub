package com.flightstats.hub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.replication.Replicator;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

@ToString
@EqualsAndHashCode(of = {"name"})
public class ChannelConfig implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new HubDateTypeAdapter()).create();
    private final String name;
    private final String owner;
    private final Date creationDate;
    private final long ttlDays;
    private final long maxItems;
    private final String description;
    private final Set<String> tags = new TreeSet<>();
    private final String replicationSource;

    public ChannelConfig(Builder builder) {
        name = StringUtils.trim(builder.name);
        owner = StringUtils.trim(builder.owner);
        creationDate = builder.creationDate;
        if (builder.maxItems == 0 && builder.ttlDays == 0) {
            ttlDays = 120;
            maxItems = 0;
        } else {
            ttlDays = builder.ttlDays;
            maxItems = builder.maxItems;
        }
        description = StringUtils.defaultString(builder.description, "");
        tags.addAll(builder.tags);
        if (StringUtils.isBlank(builder.replicationSource)) {
            replicationSource = "";
            tags.remove(Replicator.REPLICATED);
        } else {
            replicationSource = builder.replicationSource;
            tags.add(Replicator.REPLICATED);
        }
    }

    public static ChannelConfig fromJson(String json) {
        if (StringUtils.isEmpty(json)) {
            throw new InvalidRequestException("this method requires at least a json name");
        }
        return gson.fromJson(json, ChannelConfig.Builder.class).build();
    }

    public static ChannelConfig fromJsonName(String json, String name) {
        if (StringUtils.isEmpty(json)) {
            return builder().withName(name).build();
        }
        return gson.fromJson(json, ChannelConfig.Builder.class)
                .withName(name)
                .build();
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    @JsonProperty("name")
    public String getName() {
        return name;
    }

    @JsonProperty("creationDate")
    public Date getCreationDate() {
        return creationDate;
    }

    @JsonProperty("ttlDays")
    public long getTtlDays() {
        return ttlDays;
    }

    @JsonProperty("description")
    public String getDescription() {
        return description;
    }

    @JsonProperty("tags")
    public Set<String> getTags() {
        return tags;
    }

    @JsonProperty("replicationSource")
    public String getReplicationSource() {
        return replicationSource;
    }

    @JsonProperty("maxItems")
    public long getMaxItems() {
        return maxItems;
    }

    @JsonProperty("owner")
    public String getOwner() {
        return owner;
    }

    @JsonIgnore
    public boolean isReplicating() {
        return StringUtils.isNotBlank(replicationSource);
    }

    public static class Builder {
        private static final ObjectMapper mapper = new ObjectMapper();
        private String name;
        private String owner = "";
        private Date creationDate = new Date();
        private long ttlDays = 0;
        private String description = "";
        private Set<String> tags = new HashSet<>();
        private String replicationSource = "";
        private long maxItems = 0;

        public Builder() {
        }

        public Builder withChannelConfiguration(ChannelConfig config) {
            this.name = config.name;
            this.creationDate = config.creationDate;
            this.ttlDays = config.ttlDays;
            this.maxItems = config.maxItems;
            this.description = config.description;
            this.tags.addAll(config.getTags());
            this.replicationSource = config.replicationSource;
            this.owner = config.owner;
            return this;
        }

        public Builder withUpdateJson(String json) throws IOException {
            JsonNode rootNode = mapper.readTree(json);
            if (rootNode.has("owner")) {
                withOwner(getValue(rootNode.get("owner")));
            }
            if (rootNode.has("description")) {
                withDescription(getValue(rootNode.get("description")));
            }
            if (rootNode.has("ttlDays")) {
                withTtlDays(rootNode.get("ttlDays").asLong());
            }
            if (rootNode.has("maxItems")) {
                withMaxItems(rootNode.get("maxItems").asLong());
            }
            if (rootNode.has("tags")) {
                tags.clear();
                JsonNode tagsNode = rootNode.get("tags");
                for (JsonNode tagNode : tagsNode) {
                    tags.add(tagNode.asText());
                }
            }
            if (rootNode.has("replicationSource")) {
                withReplicationSource(rootNode.get("replicationSource").asText());
            }
            return this;
        }

        private String getValue(JsonNode jsonNode) {
            String value = jsonNode.asText();
            if (value.equals("null")) {
                value = "";
            }
            return value;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withTtlDays(long ttlDays) {
            this.ttlDays = ttlDays;
            return this;
        }

        public Builder withMaxItems(long maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public Builder withCreationDate(Date date) {
            this.creationDate = date;
            return this;
        }

        public Builder withTags(Collection<String> tags) {
            this.tags.addAll(tags);
            return this;
        }

        public ChannelConfig build() {
            return new ChannelConfig(this);
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withReplicationSource(String replicationSource) {
            this.replicationSource = replicationSource;
            return this;
        }

        public Builder withOwner(String owner) {
            this.owner = owner;
            return this;
        }
    }
}
