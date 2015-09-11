package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.google.common.base.Optional;

public interface GroupStrategy extends AutoCloseable {

    ContentPath getStartingPath();

    ContentPath getLastCompleted();

    void start(Group group, ContentPath startingKey);

    Optional<ContentPath> next();

    ContentPath getType();

    ObjectNode createResponse(ContentPath contentPath, ObjectMapper mapper);

    ContentPath inProcess(ContentPath contentPath);

    static ContentPath getType(Group group) {
        if (group.isMinute()) {
            return MinutePath.NONE;
        }
        return ContentKey.NONE;
    }

    static GroupStrategy getStrategy(Group group, LastContentPath lastContentPath, ChannelService channelService) {
        if (group.isMinute()) {
            return new MinuteGroupStrategy(group, lastContentPath, channelService);
        }
        return new SingleGroupStrategy(group, lastContentPath, channelService);
    }

}
