package com.flightstats.hub.group;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

public interface GroupStrategy extends AutoCloseable {

    ContentPath getStartingPath();

    ContentPath getLastCompleted();

    void start(Group group, ContentPath startingKey);

    Optional<ContentPath> next();

    ContentPath createContentPath();

    ObjectNode createResponse(ContentPath contentPath, ObjectMapper mapper);

    ContentPath inProcess(ContentPath contentPath);

    static ContentPath createContentPath(Group group) {
        if (group.isMinute()) {
            return new MinutePath();
        }
        return new ContentKey(TimeUtil.now(), "initial");
    }

    static GroupStrategy getStrategy(Group group, LastContentPath lastContentPath, ChannelService channelService) {
        if (group.isMinute()) {
            return new MinuteGroupStrategy(group, lastContentPath, channelService);
        }
        return new SingleGroupStrategy(group, lastContentPath, channelService);
    }

    static void close(AtomicBoolean shouldExit, ExecutorService executorService, BlockingQueue queue) {
        if (!shouldExit.get()) {
            shouldExit.set(true);
        }
        if (executorService != null) {
            executorService.shutdownNow();
        }
        if (queue != null) {
            queue.clear();
        }
    }
}
