package com.flightstats.hub.spoke;

import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Trace;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.Inject;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * This is the entry point in the Hub's storage system, Spoke.
 * <p>
 * It is called in-process on the originating Hub server, and this class will
 * call the Spoke servers in the cluster.
 */
public class SpokeContentDao implements ContentDao {

    private final static Logger logger = LoggerFactory.getLogger(SpokeContentDao.class);

    private final RemoteSpokeStore spokeStore;

    @Inject
    public SpokeContentDao(RemoteSpokeStore spokeStore) {
        this.spokeStore = spokeStore;
    }

    @Override
    public ContentKey write(String channelName, Content content) {
        content.getTraces().add(new Trace("SpokeContentDao.start"));
        try {
            byte[] payload = SpokeMarshaller.toBytes(content);
            content.getTraces().add(new Trace("SpokeContentDao.marshalled"));
            if (!content.getContentKey().isPresent()) {
                content.setContentKey(new ContentKey());
            }
            ContentKey key = content.getContentKey().get();
            String path = getPath(channelName, key);
            logger.trace("writing key {} to channel {}", key, channelName);
            if (!spokeStore.write(path, payload, content)) {
                logger.warn("failed to write to all for " + path);
            }
            content.getTraces().add(new Trace("SpokeContentDao.end"));
            return key;
        } catch (Exception e) {
            content.getTraces().add(new Trace("SpokeContentDao", "error", e.getMessage()));
            logger.warn("what's up?", e);
            return null;
        }
    }

    private String getPath(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
    }

    @Override
    public Content read(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        try {
            return spokeStore.read(path, key);
        } catch (Exception e) {
            logger.warn("unable to get data: " + path, e);
            return null;
        }
    }

    @Override
    public SortedSet<ContentKey> queryByTime(String channelName, DateTime startTime, TimeUtil.Unit unit) {
        logger.debug("query by time {} {} {}", channelName, startTime, unit);
        String timePath = unit.format(startTime);
        try {
            return new TreeSet<>(spokeStore.readTimeBucket(channelName, timePath));
        } catch (Exception e) {
            logger.warn("what happened? " + channelName + " " + startTime + " " + unit, e);
        }
        return new TreeSet<>();
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        SortedSet<ContentKey> orderedKeys = new TreeSet<>();
        ContentKey startKey = query.getContentKey();
        DateTime startTime = startKey.getTime();
        if (query(query, orderedKeys, startKey, startTime)) {
            return orderedKeys;
        }
        startTime = query.isNext() ? startTime.plusDays(1) : startTime.minusDays(1);
        query(query, orderedKeys, startKey, startTime);
        return orderedKeys;
    }

    private boolean query(DirectionQuery query, SortedSet<ContentKey> orderedKeys, ContentKey startKey, DateTime startTime) {
        Collection<ContentKey> queryByTime = queryByTime(query.getChannelName(), startTime, TimeUtil.Unit.DAYS);
        if (query.isNext()) {
            //from oldest to newest
            DateTime stableTime = TimeUtil.time(query.isStable());
            for (ContentKey contentKey : new TreeSet<>(queryByTime)) {
                if (contentKey.compareTo(startKey) > 0 && contentKey.getTime().isBefore(stableTime)) {
                    orderedKeys.add(contentKey);
                    if (orderedKeys.size() == query.getCount()) {
                        return true;
                    }
                }
            }
        } else {
            PreviousUtil.addToPrevious(query, queryByTime, orderedKeys);
        }
        return false;
    }

    @Override
    public void delete(String channelName) {
        try {
            spokeStore.delete(channelName);
        } catch (Exception e) {
            logger.warn("unable to delete " + channelName, e);
        }
    }

    @Override
    public void initialize() {
        //do anything?
    }
}
