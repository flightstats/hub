package com.flightstats.hub.webhook;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.NamedType;
import com.flightstats.hub.util.RequestUtils;
import de.danielbechler.diff.ObjectDifferBuilder;
import de.danielbechler.diff.node.DiffNode;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Wither;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Value
@Builder(toBuilder = true)
public class Webhook implements Comparable<Webhook>, NamedType {

    public static final String SINGLE = "SINGLE";
    public static final String MINUTE = "MINUTE";
    public static final String SECOND = "SECOND";

    private final static Logger logger = LoggerFactory.getLogger(Webhook.class);

    private final String callbackUrl;
    private final String channelUrl;
    private final boolean fastForwardable;

    @Wither
    private final Integer parallelCalls;
    @Wither
    private final String name;
    @Wither
    private final transient ContentPath startingKey;
    @Wither
    private final String batch;
    @Wither
    private final boolean heartbeat;
    @Wither
    private final boolean paused;
    @Wither
    private final Integer ttlMinutes;
    @Wither
    private final Integer maxWaitMinutes;
    @Wither
    private final Integer callbackTimeoutSeconds;

    // webhooks with tagUrl defined are prototype webhook definitions

    @Wither
    private final String tagUrl;

    // webhooks with managedByTag set were created by a tag webhook prototype and will be automatically
    // created and deleted when a channel has the webhook "tagUrl" added or removed.

    @Wither
    private final String managedByTag;
    @Wither
    private final Integer maxAttempts;
    @Wither
    private final String errorChannelUrl;

    @JsonIgnore
    boolean isTagPrototype() {
        return !StringUtils.isEmpty(this.tagUrl);
    }

    String getTagFromTagUrl() {
        return RequestUtils.getTag(this.getTagUrl());
    }

    boolean isManagedByTag() {
        return !StringUtils.isEmpty(managedByTag);
    }

    @JsonIgnore
    ContentPath getStartingKey() {
        return startingKey;
    }

    boolean allowedToChange(Webhook other) {
        return getChannelName().equals(other.getChannelName())
                && name.equals(other.name);
    }

    boolean isChanged(Webhook other) {
        DiffNode diff = ObjectDifferBuilder.buildDefault().compare(this, other);
        return !Objects.equals(parallelCalls, other.parallelCalls) || diff.hasChanges();
    }

    public boolean isMinute() {
        return MINUTE.equalsIgnoreCase(getBatch());
    }

    public boolean isSecond() {
        return SECOND.equalsIgnoreCase(getBatch());
    }

    public Integer getTtlMinutes() {
        if (ttlMinutes == null) {
            return 0;
        }
        return ttlMinutes;
    }

    @Override
    public int compareTo(Webhook other) {
        return getName().compareTo(other.getName());
    }

    public String getChannelName() {
        return RequestUtils.getChannelName(getChannelUrl());
    }

}
