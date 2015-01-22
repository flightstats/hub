package com.flightstats.hub.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.exception.InvalidRequestException;
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
public class ChannelConfiguration implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Gson gson = new GsonBuilder().registerTypeAdapter(Date.class, new HubDateTypeAdapter()).create();
    private final String name;
    private final Date creationDate;
    private final long ttlDays;
    private final String description;
    private final Set<String> tags = new TreeSet<>();
    private final String replicationSource;

    public ChannelConfiguration(Builder builder) {
        this.name = StringUtils.trim(builder.name);
        this.creationDate = builder.creationDate;
        this.ttlDays = builder.ttlDays;
        this.description = StringUtils.defaultString(builder.description, "");
        this.tags.addAll(builder.tags);
        if (StringUtils.isBlank(builder.replicationSource)) {
            this.replicationSource = "";
            tags.remove("replicated");
        } else {
            this.replicationSource = builder.replicationSource;
            tags.add("replicated");
        }
    }

    public static ChannelConfiguration fromJson(String json) {
        if (StringUtils.isEmpty(json)) {
            throw new InvalidRequestException("this method requires at least a json name");
        }
        return gson.fromJson(json, ChannelConfiguration.Builder.class).build();
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

    public static class Builder {
        private static final ObjectMapper mapper = new ObjectMapper();
        private String name;
        private Date creationDate = new Date();
        private long ttlDays = 120;
        private String description = "";
        private Set<String> tags = new HashSet<>();
        private String replicationSource = "";

        public Builder() {
        }

        public Builder withChannelConfiguration(ChannelConfiguration config) {
            this.name = config.name;
            this.creationDate = config.creationDate;
            this.ttlDays = config.ttlDays;
            this.description = config.description;
            this.tags.addAll(config.getTags());
            this.replicationSource = config.replicationSource;
            return this;
        }

        public Builder withUpdateJson(String json) throws IOException {
            JsonNode rootNode = mapper.readTree(json);
            if (rootNode.has("description")) {
                String desc = rootNode.get("description").asText();
                if (desc.equals("null")) {
                    desc = "";
                }
                withDescription(desc);
            }
            if (rootNode.has("ttlDays")) {
                withTtlDays(rootNode.get("ttlDays").asLong());
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

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withTtlDays(long ttlDays) {
            this.ttlDays = ttlDays;
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

        public ChannelConfiguration build() {
            return new ChannelConfiguration(this);
        }

        public Builder withDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder withReplicationSource(String replicationSource) {
            this.replicationSource = replicationSource;
            return this;
        }
    }
}
