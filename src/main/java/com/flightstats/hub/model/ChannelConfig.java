package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.util.TimeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.flightstats.hub.model.BuiltInTag.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Value
@Builder(toBuilder = true)
public class ChannelConfig implements Serializable, NamedType {

    public static final String SINGLE = "SINGLE";
    public static final String BATCH = "BATCH";
    public static final String BOTH = "BOTH";

    private static final long serialVersionUID = 1L;

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new HubDateTypeAdapter())
            .registerTypeAdapter(DateTime.class, new HubDateTimeTypeAdapter())
            .create();
    private static final ObjectMapper mapper = new ObjectMapper();

    private final String name;
    private final String owner;
    private final Date creationDate;
    private final long ttlDays;
    private final long maxItems;
    private final String description;
    private final Set<String> tags;
    private final String replicationSource;
    private final String storage;
    private final GlobalConfig global;
    private final boolean protect;
    private final DateTime mutableTime;

    private ChannelConfig(String name, String owner, Date creationDate, long ttlDays, long maxItems, String description,
                          Set<String> tags, String replicationSource, String storage, GlobalConfig global,
                          boolean protect, DateTime mutableTime) {
        this.name = StringUtils.trim(name);
        this.owner = StringUtils.trim(owner);
        this.creationDate = creationDate;
        this.description = description;
        this.tags = tags;
        this.replicationSource = replicationSource;
        this.mutableTime = mutableTime;

        if (maxItems == 0 && ttlDays == 0 && mutableTime == null) {
            this.ttlDays = 120;
            this.maxItems = 0;
        } else {
            this.ttlDays = ttlDays;
            this.maxItems = maxItems;
        }

        if (isBlank(storage)) {
            this.storage = SINGLE;
        } else {
            this.storage = StringUtils.upperCase(storage);
        }

        if (global != null) {
            this.global = global.cleanup();
        } else {
            this.global = null;
        }

        addTagIf(!isBlank(replicationSource), REPLICATED);
        addTagIf(isGlobal(), GLOBAL);
        addTagIf(isHistorical(), HISTORICAL);

        if (HubProperties.isProtected()) {
            this.protect = true;
        } else {
            this.protect = protect;
        }
    }

    private void addTagIf(boolean shouldBeTagged, BuiltInTag tag) {
        if (shouldBeTagged) {
            tags.add(tag.toString());
        } else {
            tags.remove(tag.toString());
        }
    }

    public static ChannelConfig createFromJson(String json) {
        if (StringUtils.isEmpty(json)) {
            throw new InvalidRequestException("this method requires at least a json name");
        } else {
            return gson.fromJson(json, ChannelConfig.ChannelConfigBuilder.class).build();
        }
    }

    public static ChannelConfig createFromJsonWithName(String json, String name) {
        if (StringUtils.isEmpty(json)) {
            return builder().name(name).build();
        } else {
            return gson.fromJson(json, ChannelConfig.ChannelConfigBuilder.class).name(name).build();
        }
    }

    public static ChannelConfig updateFromJson(ChannelConfig config, String json) {
        ChannelConfigBuilder builder = config.toBuilder();
        JsonNode rootNode = readJSON(json);

        if (rootNode.has("owner")) builder.owner(getString(rootNode.get("owner")));
        if (rootNode.has("description")) builder.description(getString(rootNode.get("description")));
        if (rootNode.has("ttlDays")) builder.ttlDays(rootNode.get("ttlDays").asLong());
        if (rootNode.has("maxItems")) builder.maxItems(rootNode.get("maxItems").asLong());
        if (rootNode.has("tags")) builder.tags(getSet(rootNode.get("tags")));
        if (rootNode.has("replicationSource")) builder.replicationSource(getString(rootNode.get("replicationSource")));
        if (rootNode.has("storage")) builder.storage(getString(rootNode.get("storage")));
        if (rootNode.has("global")) builder.global(GlobalConfig.parseJson(rootNode.get("global")));
        if (rootNode.has("protect")) builder.protect(rootNode.get("protect").asBoolean());
        if (rootNode.has("mutableTime")) {
            builder.mutableTime(HubDateTimeTypeAdapter.deserialize(rootNode.get("mutableTime").asText()));
        }
        return builder.build();
    }

    private static JsonNode readJSON(String json) {
        try {
            return mapper.readTree(json);
        } catch (Exception e) {
            throw new InvalidRequestException("couldn't read json: " + json);
        }
    }

    private static String getString(JsonNode node) {
        String value = node.asText();
        if (value.equals("null")) {
            value = "";
        }
        return value;
    }

    private static Set<String> getSet(JsonNode node) {
        if (!node.isArray()) throw new InvalidRequestException("json node is not an array: " + node.toString());
        return StreamSupport.stream(node.spliterator(), false)
                .map(JsonNode::asText)
                .collect(Collectors.toSet());
    }

    public String toJson() {
        return gson.toJson(this);
    }

    public DateTime getTtlTime() {
        if (isHistorical()) {
            return mutableTime.plusMillis(1);
        }
        return TimeUtil.getEarliestTime(ttlDays);
    }

    public boolean isGlobal() {
        return global != null;
    }

    public boolean isGlobalMaster() {
        return isGlobal() && global.isMaster();
    }

    public boolean isGlobalSatellite() {
        return isGlobal() && !global.isMaster();
    }

    public boolean isReplicating() {
        return StringUtils.isNotBlank(replicationSource) || isGlobalSatellite();
    }

    public boolean isLive() {
        return !isReplicating();
    }

    public boolean isValidStorage() {
        return storage.equals(SINGLE) || storage.equals(BATCH) || storage.equals(BOTH);
    }

    public boolean isSingle() {
        return storage.equals(SINGLE);
    }

    public boolean isBatch() {
        return storage.equals(BATCH);
    }

    public boolean isBoth() {
        return storage.equals(BOTH);
    }

    public boolean isHistorical() {
        return mutableTime != null;
    }

    @SuppressWarnings("unused")
    public static class ChannelConfigBuilder {
        private String owner = "";
        private Date creationDate = new Date();
        private String description = "";
        private TreeSet<String> tags = new TreeSet<>();
        private String replicationSource = "";
        private String storage = "";
        private boolean protect = HubProperties.isProtected();

        public ChannelConfigBuilder tags(List<String> tagList) {
            this.tags.clear();
            this.tags.addAll(tagList.stream().map(Function.identity()).collect(Collectors.toSet()));
            return this;
        }

        public ChannelConfigBuilder tags(Set<String> tagSet) {
            this.tags.clear();
            this.tags.addAll(tagSet);
            return this;
        }
    }
}
