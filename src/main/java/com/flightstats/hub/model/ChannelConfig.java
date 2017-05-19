package com.flightstats.hub.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.util.TimeUtil;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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

    String name;
    String owner;
    Date creationDate;
    long ttlDays;
    long maxItems;
    boolean keepForever;
    String description;
    Set<String> tags;
    String replicationSource;
    String storage;
    GlobalConfig global;
    boolean protect;
    DateTime mutableTime;
    boolean allowZeroBytes;

    private ChannelConfig(String name, String owner, Date creationDate, long ttlDays, long maxItems, boolean keepForever, String description,
                          Set<String> tags, String replicationSource, String storage, GlobalConfig global,
                          boolean protect, DateTime mutableTime, boolean allowZeroBytes) {
        this.name = StringUtils.trim(name);
        this.owner = StringUtils.trim(owner);
        this.creationDate = creationDate;
        this.description = description;
        this.tags = tags;
        this.replicationSource = replicationSource;
        this.mutableTime = mutableTime;
        this.allowZeroBytes = allowZeroBytes;
        this.keepForever = keepForever;  // keepForever overrides all other retention policies
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

    public static ChannelConfigBuilder builder() {
        return new ChannelConfigBuilder();
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
        if (rootNode.has("keepForever")) builder.keepForever(rootNode.get("keepForever").asBoolean());
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
        if (rootNode.has("allowZeroBytes")) builder.allowZeroBytes(rootNode.get("allowZeroBytes").asBoolean());
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
        //bc todo: keepForever - what should we return if keep forever is set?
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

    public String getName() {
        return this.name;
    }

    public String getOwner() {
        return this.owner;
    }

    public Date getCreationDate() {
        return this.creationDate;
    }

    public boolean getKeepForever() {
        return this.keepForever;
    }

    public long getTtlDays() {
        return this.ttlDays;
    }

    public long getMaxItems() {
        return this.maxItems;
    }

    public String getDescription() {
        return this.description;
    }

    public Set<String> getTags() {
        return this.tags;
    }

    public String getReplicationSource() {
        return this.replicationSource;
    }

    public String getStorage() {
        return this.storage;
    }

    public GlobalConfig getGlobal() {
        return this.global;
    }

    public boolean isProtect() {
        return this.protect;
    }

    public DateTime getMutableTime() {
        return this.mutableTime;
    }

    public boolean isAllowZeroBytes() {
        return this.allowZeroBytes;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof ChannelConfig)) return false;
        final ChannelConfig other = (ChannelConfig) o;
        final Object this$name = this.getName();
        final Object other$name = other.getName();
        if (this$name == null ? other$name != null : !this$name.equals(other$name)) return false;
        final Object this$owner = this.getOwner();
        final Object other$owner = other.getOwner();
        if (this$owner == null ? other$owner != null : !this$owner.equals(other$owner)) return false;
        final Object this$creationDate = this.getCreationDate();
        final Object other$creationDate = other.getCreationDate();
        if (this$creationDate == null ? other$creationDate != null : !this$creationDate.equals(other$creationDate))
            return false;
        if (this.getKeepForever() != other.getKeepForever()) return false;
        if (this.getTtlDays() != other.getTtlDays()) return false;
        if (this.getMaxItems() != other.getMaxItems()) return false;
        final Object this$description = this.getDescription();
        final Object other$description = other.getDescription();
        if (this$description == null ? other$description != null : !this$description.equals(other$description))
            return false;
        final Object this$tags = this.getTags();
        final Object other$tags = other.getTags();
        if (this$tags == null ? other$tags != null : !this$tags.equals(other$tags)) return false;
        final Object this$replicationSource = this.getReplicationSource();
        final Object other$replicationSource = other.getReplicationSource();
        if (this$replicationSource == null ? other$replicationSource != null : !this$replicationSource.equals(other$replicationSource))
            return false;
        final Object this$storage = this.getStorage();
        final Object other$storage = other.getStorage();
        if (this$storage == null ? other$storage != null : !this$storage.equals(other$storage)) return false;
        final Object this$global = this.getGlobal();
        final Object other$global = other.getGlobal();
        if (this$global == null ? other$global != null : !this$global.equals(other$global)) return false;
        if (this.isProtect() != other.isProtect()) return false;
        final Object this$mutableTime = this.getMutableTime();
        final Object other$mutableTime = other.getMutableTime();
        if (this$mutableTime == null ? other$mutableTime != null : !this$mutableTime.equals(other$mutableTime))
            return false;
        if (this.isAllowZeroBytes() != other.isAllowZeroBytes()) return false;
        return true;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $name = this.getName();
        result = result * PRIME + ($name == null ? 43 : $name.hashCode());
        final Object $owner = this.getOwner();
        result = result * PRIME + ($owner == null ? 43 : $owner.hashCode());
        final Object $creationDate = this.getCreationDate();
        result = result * PRIME + ($creationDate == null ? 43 : $creationDate.hashCode());
        final boolean $keepForever = this.getKeepForever();
        result = result * PRIME + ($keepForever ? 1 : 0);
        final long $ttlDays = this.getTtlDays();
        result = result * PRIME + (int) ($ttlDays >>> 32 ^ $ttlDays);
        final long $maxItems = this.getMaxItems();
        result = result * PRIME + (int) ($maxItems >>> 32 ^ $maxItems);
        final Object $description = this.getDescription();
        result = result * PRIME + ($description == null ? 43 : $description.hashCode());
        final Object $tags = this.getTags();
        result = result * PRIME + ($tags == null ? 43 : $tags.hashCode());
        final Object $replicationSource = this.getReplicationSource();
        result = result * PRIME + ($replicationSource == null ? 43 : $replicationSource.hashCode());
        final Object $storage = this.getStorage();
        result = result * PRIME + ($storage == null ? 43 : $storage.hashCode());
        final Object $global = this.getGlobal();
        result = result * PRIME + ($global == null ? 43 : $global.hashCode());
        result = result * PRIME + (this.isProtect() ? 79 : 97);
        final Object $mutableTime = this.getMutableTime();
        result = result * PRIME + ($mutableTime == null ? 43 : $mutableTime.hashCode());
        result = result * PRIME + (this.isAllowZeroBytes() ? 79 : 97);
        return result;
    }

    public String toString() {
        return "com.flightstats.hub.model.ChannelConfig(name=" + this.getName() + ", owner=" + this.getOwner() + ", creationDate=" + this.getCreationDate() + ", ttlDays=" + this.getTtlDays() + ", maxItems=" + this.getMaxItems() + ", description=" + this.getDescription() + ", tags=" + this.getTags() + ", replicationSource=" + this.getReplicationSource() + ", storage=" + this.getStorage() + ", global=" + this.getGlobal() + ", protect=" + this.isProtect() + ", mutableTime=" + this.getMutableTime() + ", allowZeroBytes=" + this.isAllowZeroBytes() + ")";
    }

    public ChannelConfigBuilder toBuilder() {
        return new ChannelConfigBuilder(this);
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
        private boolean allowZeroBytes = true;
        private String name;
        private boolean keepForever;
        private long ttlDays;
        private long maxItems;
        private GlobalConfig global;
        private DateTime mutableTime;

        ChannelConfigBuilder() {
        }

        ChannelConfigBuilder(ChannelConfig config) {
            owner(config.getOwner());
            creationDate(config.getCreationDate());
            description(config.getDescription());
            tags(config.getTags());
            replicationSource(config.getReplicationSource());
            storage(config.getStorage());
            protect(config.isProtect());
            allowZeroBytes(config.isAllowZeroBytes());
            name(config.getName());
            keepForever(config.getKeepForever());
            ttlDays(config.getTtlDays());
            maxItems(config.getMaxItems());
            global(config.getGlobal());
            mutableTime(config.getMutableTime());
        }

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

        public ChannelConfigBuilder name(String name) {
            this.name = name;
            return this;
        }

        public ChannelConfigBuilder owner(String owner) {
            this.owner = owner;
            return this;
        }

        public ChannelConfigBuilder creationDate(Date creationDate) {
            this.creationDate = creationDate;
            return this;
        }

        public ChannelConfigBuilder keepForever(boolean keepForever) {
            this.keepForever = keepForever;
            return this;
        }

        public ChannelConfigBuilder ttlDays(long ttlDays) {
            this.ttlDays = ttlDays;
            return this;
        }

        public ChannelConfigBuilder maxItems(long maxItems) {
            this.maxItems = maxItems;
            return this;
        }

        public ChannelConfigBuilder description(String description) {
            this.description = description;
            return this;
        }

        public ChannelConfigBuilder replicationSource(String replicationSource) {
            this.replicationSource = replicationSource;
            return this;
        }

        public ChannelConfigBuilder storage(String storage) {
            this.storage = storage;
            return this;
        }

        public ChannelConfigBuilder global(GlobalConfig global) {
            this.global = global;
            return this;
        }

        public ChannelConfigBuilder protect(boolean protect) {
            this.protect = protect;
            return this;
        }

        public ChannelConfigBuilder mutableTime(DateTime mutableTime) {
            this.mutableTime = mutableTime;
            return this;
        }

        public ChannelConfigBuilder allowZeroBytes(boolean allowZeroBytes) {
            this.allowZeroBytes = allowZeroBytes;
            return this;
        }

        public ChannelConfig build() {
            return new ChannelConfig(name, owner, creationDate, ttlDays, maxItems, keepForever, description, tags, replicationSource, storage, global, protect, mutableTime, allowZeroBytes);
        }

        public String toString() {
            return "com.flightstats.hub.model.ChannelConfig.ChannelConfigBuilder(owner=" + this.owner + ", creationDate=" + this.creationDate + ", description=" + this.description + ", tags=" + this.tags + ", replicationSource=" + this.replicationSource + ", storage=" + this.storage + ", protect=" + this.protect + ", allowZeroBytes=" + this.allowZeroBytes + ", name=" + this.name + ", ttlDays=" + this.ttlDays + ", maxItems=" + this.maxItems + ", global=" + this.global + ", mutableTime=" + this.mutableTime + ")";
        }


    }
}
