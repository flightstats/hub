package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.google.common.base.Optional;

public interface Caller extends AutoCloseable {

    ContentPath getStartingPath();

    ContentPath getLastCompleted();

    void start(Group group, ContentPath startingKey);

    Optional<ContentPath> next();

    ContentPath getType();

    ObjectNode createResponse(ContentPath key, ObjectMapper mapper);

    static ContentPath getType(Group group) {
        return ContentKey.NONE;
    }
}
