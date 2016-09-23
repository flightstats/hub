package com.flightstats.hub.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.util.TimeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.io.IOException;
import java.io.Serializable;
import java.util.*;

@ToString
@EqualsAndHashCode(of = {"name"})
public class ChannelConfig implements Serializable, NamedType {

    public static final String SINGLE = "SINGLE";
    public static final String BATCH = "BATCH";
    public static final String BOTH = "BOTH";
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
    private final String storage;
    private final GlobalConfig global;
    private final boolean historical;
    private final boolean allowDataLoss;

    private ChannelConfig(Builder builder) {
        name = StringUtils.trim(builder.name);
        owner = StringUtils.trim(builder.owner);
        creationDate = builder.creationDate;
        historical = builder.historical;
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
            tags.remove(BuiltInTag.REPLICATED.toString());
        } else {
            replicationSource = builder.replicationSource;
            tags.add(BuiltInTag.REPLICATED.toString());
        }
        if (StringUtils.isBlank(builder.storage)) {
            storage = SINGLE;
        } else {
            storage = StringUtils.upperCase(builder.storage);
        }
        if (builder.global != null) {
            global = builder.global.cleanup();
        } else {
            global = null;
        }
        if (isGlobal()) {
            tags.add(BuiltInTag.GLOBAL.toString());
        } else {
            tags.remove(BuiltInTag.GLOBAL.toString());
        }
        if (isHistorical()) {
            tags.add(BuiltInTag.HISTORICAL.toString());
        } else {
            tags.remove(BuiltInTag.HISTORICAL.toString());
        }
        if (HubProperties.allowDataLoss()) {
            allowDataLoss = builder.allowDataLoss;
        } else {
            allowDataLoss = false;
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

    public static Builder builder() {
        return new Builder();
    }

    public String toJson() {
        return gson.toJson(this);
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

    @JsonIgnore
    public DateTime getTtlTime() {
        if (historical) {
            return TimeUtil.now().minusDays((int) ttlDays);
        }
        return TimeUtil.getEarliestTime(ttlDays);
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

    @JsonProperty("storage")
    public String getStorage() {
        return storage;
    }

    @JsonProperty("global")
    public GlobalConfig getGlobal() {
        return global;
    }

    @JsonProperty("allowDataLoss")
    public boolean isAllowDataLoss() {
        return allowDataLoss;
    }

    @JsonIgnore
    public boolean isGlobal() {
        return global != null;
    }

    @JsonIgnore
    public boolean isGlobalMaster() {
        return isGlobal() && global.isMaster();
    }

    @JsonIgnore
    public boolean isGlobalSatellite() {
        return isGlobal() && !global.isMaster();
    }

    @JsonIgnore
    public boolean isReplicating() {
        return StringUtils.isNotBlank(replicationSource) || isGlobalSatellite();
    }

    public boolean isHistorical() {
        return historical;
    }

    @JsonIgnore
    public boolean isLive() {
        return !isHistorical() && !isReplicating();
    }

    @JsonIgnore
    public boolean isValidStorage() {
        return storage.equals(SINGLE) || storage.equals(BATCH) || storage.equals(BOTH);
    }

    @JsonIgnore
    public boolean isSingle() {
        return storage.equals(SINGLE);
    }

    @JsonIgnore
    public boolean isBatch() {
        return storage.equals(BATCH);
    }

    @JsonIgnore
    public boolean isBoth() {
        return storage.equals(BOTH);
    }

    public boolean hasChanged(ChannelConfig otherConfig) {
        if (otherConfig == null) {
            return true;
        }
        if (!StringUtils.equals(getOwner(), otherConfig.getOwner())) {
            return true;
        }
        if (!StringUtils.equals(getDescription(), otherConfig.getDescription())) {
            return true;
        }
        if (!StringUtils.equals(getReplicationSource(), otherConfig.getReplicationSource())) {
            return true;
        }
        if (!StringUtils.equals(getStorage(), otherConfig.getStorage())) {
            return true;
        }
        if (getTtlDays() != otherConfig.getTtlDays()) {
            return true;
        }
        if (getMaxItems() != otherConfig.getMaxItems()) {
            return true;
        }
        if (!getTags().equals(otherConfig.getTags())) {
            return true;
        }
        if (isHistorical() != otherConfig.isHistorical()) {
            return true;
        }
        if (allowDataLoss != otherConfig.allowDataLoss) {
            return true;
        }
        return false;
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
        private String storage;
        private GlobalConfig global;
        private boolean historical;
        private boolean allowDataLoss = HubProperties.allowDataLoss();

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
            this.storage = config.storage;
            this.global = config.global;
            this.historical = config.historical;
            this.allowDataLoss = config.allowDataLoss;
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
            if (rootNode.has("storage")) {
                withStorage(rootNode.get("storage").asText());
            }
            if (rootNode.has("global")) {
                global = GlobalConfig.parseJson(rootNode.get("global"));
            }
            if (rootNode.has("historical")) {
                withHistorical(rootNode.get("historical").asBoolean());
            }
            if (rootNode.has("allowDataLoss")) {
                allowDataLoss = rootNode.get("allowDataLoss").asBoolean();
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

        public Builder withStorage(String storage) {
            this.storage = storage;
            return this;
        }

        public Builder withGlobal(GlobalConfig global) {
            this.global = global;
            return this;
        }

        public Builder withHistorical(boolean historical) {
            this.historical = historical;
            return this;
        }

        public Builder withAllowDataLoss(boolean allowDataLoss) {
            this.allowDataLoss = allowDataLoss;
            return this;
        }
    }
}
