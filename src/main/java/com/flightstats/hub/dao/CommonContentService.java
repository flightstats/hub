package com.flightstats.hub.dao;

import com.diffplug.common.base.Errors;
import com.flightstats.hub.app.InFlightService;
import com.flightstats.hub.dao.aws.MultiPartParser;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.time.TimeService;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;

/**
 * Performs common logic for all ContentServices.
 */
public class CommonContentService implements ContentService {
    private final static Logger logger = LoggerFactory.getLogger(CommonContentService.class);

    @Inject
    private TimeService timeService;

    @Inject
    @Named(ContentService.IMPL)
    private ContentService contentService;

    @Inject
    private InFlightService inFlightService;

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        return inFlightService.inFlight(Errors.rethrow().wrap(() -> doInsert(channelName, content)));
    }

    private ContentKey doInsert(String channelName, Content content) throws Exception {
        Traces traces = ActiveTraces.getLocal();
        traces.add("ContentService.insert");
        try {
            content.packageStream();
            traces.add("ContentService.insert marshalled");
            ContentKey key = content.keyAndStart(timeService.getNow());
            logger.trace("writing key {} to channel {}", key, channelName);
            contentService.insert(channelName, content);
            traces.add("ContentService.insert end", key);
            return key;
        } catch (ContentTooLargeException e) {
            logger.info("content too large for channel " + channelName);
            throw e;
        } catch (Exception e) {
            traces.add("ContentService.insert", "error", e.getMessage());
            logger.warn("insertion error " + channelName, e);
            throw e;
        }
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        return inFlightService.inFlight(Errors.rethrow().wrap(() -> {
            MultiPartParser multiPartParser = new MultiPartParser(bulkContent);
            multiPartParser.parse();
            return contentService.insert(bulkContent);
        }));
    }

    @Override
    public boolean historicalInsert(String channelName, Content content) throws Exception {
        return inFlightService.inFlight(Errors.rethrow().wrap(() -> {
            content.packageStream();
            return contentService.historicalInsert(channelName, content);
        }));
    }

    @Override
    public Optional<Content> get(String channelName, ContentKey key) {
        return contentService.get(channelName, key);
    }

    @Override
    public void get(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        contentService.get(channel, keys, callback);
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery timeQuery) {
        return contentService.queryByTime(timeQuery);
    }

    @Override
    public void delete(String channelName) {
        contentService.delete(channelName);
    }

    @Override
    public void delete(String channelName, ContentKey contentKey) {
        contentService.delete(channelName, contentKey);
    }

    @Override
    public Collection<ContentKey> queryDirection(DirectionQuery query) {
        return contentService.queryDirection(query);
    }

    @Override
    public Optional<ContentKey> getLatest(DirectionQuery query) {
        return contentService.getLatest(query);
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        contentService.deleteBefore(name, limitKey);
    }

    @Override
    public void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        contentService.notify(newConfig, oldConfig);
    }

}
