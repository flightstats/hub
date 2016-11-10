package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.CommonContentService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.function.Consumer;

public class SpokeContentService implements ContentService {

    @Inject
    @Named(ContentDao.CACHE)
    private ContentDao spokeContentDao;

    public SpokeContentService() {
        HubServices.registerPreStop(new SpokeS3ContentServiceInit());
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        return spokeContentDao.insert(channelName, content);
    }


    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        return spokeContentDao.insert(bulkContent);
    }

    @Override
    public boolean historicalInsert(String channelName, Content content) throws Exception {
        insert(channelName, content);
        return true;
    }

    @Override
    public Optional<Content> get(String channelName, ContentKey key) {
        return Optional.fromNullable(spokeContentDao.get(channelName, key));
    }

    @Override
    public void get(String channelName, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        SortedSet<MinutePath> minutePaths = ContentKeyUtil.convert(keys);
        for (MinutePath minutePath : minutePaths) {
            getValues(channelName, callback, minutePath);
        }
    }

    //todo gfm - same as SpokeS3ContentService
    private void getValues(String channelName, Consumer<Content> callback, ContentPathKeys contentPathKeys) {
        for (ContentKey contentKey : contentPathKeys.getKeys()) {
            Optional<Content> contentOptional = get(channelName, contentKey);
            if (contentOptional.isPresent()) {
                callback.accept(contentOptional.get());
            }
        }
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery query) {
        return CommonContentService.query(contentDao -> contentDao.queryByTime(query),
                Collections.singletonList(spokeContentDao));
    }

    @Override
    public Collection<ContentKey> queryDirection(DirectionQuery query) {
        return CommonContentService.query(contentDao -> contentDao.query(query),
                Collections.singletonList(spokeContentDao));
    }


    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces, boolean stable) {
        return spokeContentDao.getLatest(channel, limitKey, traces);
    }

    @Override
    public void delete(String channelName) {
        spokeContentDao.delete(channelName);
    }

    private class SpokeS3ContentServiceInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            spokeContentDao.initialize();
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing
        }
    }

}
