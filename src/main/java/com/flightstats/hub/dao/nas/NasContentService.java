package com.flightstats.hub.dao.nas;

import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.spoke.FileSpokeStore;
import com.flightstats.hub.spoke.SpokeMarshaller;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

public class NasContentService implements ContentService {
    private final static Logger logger = LoggerFactory.getLogger(NasContentService.class);

    private final FileSpokeStore fileSpokeStore;

    public NasContentService() {
        String contentPath = NasUtil.getStoragePath() + "content/";
        logger.info("using {}", contentPath);
        fileSpokeStore = new FileSpokeStore(contentPath);
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        content.getTraces().add(new Trace("NasContentService.start"));
        try {
            byte[] payload = SpokeMarshaller.toBytes(content, false);
            content.getTraces().add(new Trace("NasContentService.marshalled"));
            ContentKey key = content.keyAndStart();
            String path = getPath(channelName, key);
            logger.trace("writing key {} to channel {}", key, channelName);
            if (!fileSpokeStore.write(path, payload)) {
                logger.warn("failed to  for " + path);
            }
            content.getTraces().add(new Trace("NasContentService.end"));
            return key;
        } catch (ContentTooLargeException e) {
            logger.info("content too large for channel " + channelName);
            throw e;
        } catch (Exception e) {
            content.getTraces().add(new Trace("NasContentService", "error", e.getMessage()));
            logger.warn("insertion error " + channelName, e);
            throw e;
        }
    }

    @Override
    public Collection<ContentKey> insert(String channelName, BatchContent content) throws Exception {
        //todo - gfm - 8/14/15 -
        return null;
    }

    private String getPath(String channelName, ContentKey key) {
        return channelName + "/" + key.toUrl();
    }

    @Override
    public Optional<Content> getValue(String channelName, ContentKey key) {
        String path = getPath(channelName, key);
        try {
            byte[] bytes = fileSpokeStore.read(path);
            if (null != bytes) {
                return Optional.of(SpokeMarshaller.toContent(bytes, key));
            }
        } catch (Exception e) {
            logger.warn("unable to get data: " + path, e);
        }
        return Optional.absent();
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery query) {
        String path = query.getChannelName() + "/" + query.getUnit().format(query.getStartTime());
        Traces traces = ActiveTraces.getLocal();
        traces.add("query by time", path);
        TreeSet<ContentKey> keySet = new TreeSet<>();
        ContentKeyUtil.convertKeyStrings(fileSpokeStore.readKeysInBucket(path), keySet);
        traces.add(query.getChannelName(), keySet);
        return keySet;
    }

    @Override
    public void delete(String channelName) {
        try {
            fileSpokeStore.delete(channelName);
        } catch (Exception e) {
            logger.warn("unable to delete channel " + channelName, e);
        }
    }

    @Override
    public Collection<ContentKey> queryDirection(DirectionQuery query) {
        TreeSet<ContentKey> keys = new TreeSet<>();
        TimeUtil.Unit hours = TimeUtil.Unit.HOURS;
        DateTime time = query.getContentKey().getTime();
        if (query.isNext()) {
            handleNext(query, keys);
        } else {
            DateTime limitTime = TimeUtil.getEarliestTime((int) query.getTtlDays()).minusDays(1);
            while (keys.size() < query.getCount() && time.isAfter(limitTime)) {
                addKeys(query, keys, hours, time);
                time = time.minus(hours.getDuration());
            }
        }
        return keys;
    }

    private void handleNext(DirectionQuery query, Set<ContentKey> keys) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            fileSpokeStore.getNext(query.getChannelName(), query.getContentKey().toUrl(), query.getCount(), baos);
            String keyString = baos.toString();
            ContentKeyUtil.convertKeyStrings(keyString, keys);
        } catch (IOException e) {
            logger.warn("wah?" + query, e);
        }
    }

    private void addKeys(DirectionQuery query, TreeSet<ContentKey> keys, TimeUtil.Unit hours, DateTime time) {
        String path = query.getChannelName() + "/" + hours.format(time);
        String readKeysInBucket = fileSpokeStore.readKeysInBucket(path);
        ContentKeyUtil.convertKeyStrings(fileSpokeStore.readKeysInBucket(path), keys);
    }

    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces) {
        return ContentKeyUtil.convertKey(fileSpokeStore.getLatest(channel, limitKey.toUrl()));
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        throw new UnsupportedOperationException("deleteBefore is not supported");
    }

    public void enforceTtl(String channelName, DateTime dateTime) {
        logger.info("enforcing ttl for {} at {}", channelName, dateTime);
        fileSpokeStore.enforceTtl(channelName, dateTime);
    }
}
