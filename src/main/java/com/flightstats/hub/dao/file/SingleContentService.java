package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.dao.ContentMarshaller;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.exception.FailedWriteException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.StreamResults;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.spoke.FileSpokeStore;
import com.flightstats.hub.util.TimeUtil;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Consumer;

/**
 * SingleContentService allows for the singleHub to have different characteristics than using Spoke in the clustered hub.
 * Spoke is designed to hold a short period's cache, while the singleHub may hold data spanning much large periods of time.
 */
@Slf4j
public class SingleContentService implements ContentService {

    private final FileSpokeStore fileSpokeStore;

    @Inject
    public SingleContentService(@Named("WRITE") FileSpokeStore fileSpokeStore){
        this.fileSpokeStore = fileSpokeStore;
    }

    @Override
    public ContentKey insert(String channelName, Content content) {
        channelName = formatChannel(channelName);
        ContentKey key = content.getContentKey().get();
        String path = getPath(channelName, content.getContentKey().get());
        if (!fileSpokeStore.insert(path, content.getData())) {
            throw new FailedWriteException("unable to write to file system, " + path);
        }
        return key;
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        bulkContent.withChannel(formatChannel(bulkContent.getChannel()));
        Collection<ContentKey> keys = new ArrayList<>();
        log.info("inserting {}", bulkContent);
        for (Content content : bulkContent.getItems()) {
            log.debug("inserting item key {}", content.getContentKey().get());
            content.packageStream();
            keys.add(insert(bulkContent.getChannel(), content));
        }
        return keys;
    }

    @Override
    public boolean historicalInsert(String channelName, Content content) {
        insert(channelName, content);
        return true;
    }

    private String getPath(String channelName, ContentKey key) {
        return formatChannel(channelName) + "/" + key.toUrl();
    }

    @Override
    public Optional<Content> get(String channelName, ContentKey key, boolean cached) {
        String path = getPath(channelName, key);
        try {
            byte[] bytes = fileSpokeStore.read(path);
            if (null != bytes) {
                return Optional.of(ContentMarshaller.toContent(bytes, key));
            }
        } catch (Exception e) {
            log.warn("unable to get data: " + path, e);
        }
        return Optional.empty();
    }

    @Override
    public void get(StreamResults streamResults) {
        List<ContentKey> keys = new ArrayList<>(streamResults.getKeys());
        Consumer<Content> callback = streamResults.getCallback();
        if (streamResults.isDescending()) {
            Collections.reverse(keys);
        }
        for (ContentKey key : keys) {
            Optional<Content> contentOptional = get(streamResults.getChannel(), key, false);
            contentOptional.ifPresent(callback::accept);
        }
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery query) {
        query = query.withChannelName(formatChannel(query.getChannelName()));
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
            fileSpokeStore.delete(formatChannel(channelName));
        } catch (Exception e) {
            log.warn("unable to delete channel " + channelName, e);
        }
    }

    private String formatChannel(String channelName) {
        return channelName.toLowerCase();
    }

    @Override
    public void delete(String channelName, ContentKey key) {
        try {
            String path = getPath(channelName, key);
            boolean delete = fileSpokeStore.deleteFile(path);
            ActiveTraces.getLocal().add("SingleContentService.delete", delete, path);
        } catch (Exception e) {
            log.warn("deletion error", e);
        }
    }

    @Override
    public Collection<ContentKey> queryDirection(DirectionQuery query) {
        query = query.withChannelName(formatChannel(query.getChannelName()));
        SortedSet<ContentKey> keys = new TreeSet<>();
        TimeUtil.Unit hours = TimeUtil.Unit.HOURS;
        DateTime time = query.getStartKey().getTime();
        if (query.isNext()) {
            handleNext(query, keys);
        } else {
            DateTime limitTime = query.getEarliestTime().minusDays(1);
            while (keys.size() < query.getCount() && time.isAfter(limitTime)) {
                addKeys(query, keys, hours, time);
                keys = ContentKeyUtil.filter(keys, query);
                time = time.minus(hours.getDuration());
            }
        }
        return keys;
    }

    private void handleNext(DirectionQuery query, Set<ContentKey> keys) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            fileSpokeStore.getNext(query.getChannelName(), query.getStartKey().toUrl(), query.getCount(), baos);
            String keyString = baos.toString();
            ContentKeyUtil.convertKeyStrings(keyString, keys);
        } catch (IOException e) {
            log.warn("wah?" + query, e);
        }
    }

    private void addKeys(DirectionQuery query, Collection<ContentKey> keys, TimeUtil.Unit hours, DateTime time) {
        String path = query.getChannelName() + "/" + hours.format(time);
        ContentKeyUtil.convertKeyStrings(fileSpokeStore.readKeysInBucket(path), keys);
    }

    @Override
    public Optional<ContentKey> getLatest(DirectionQuery query) {
        return ContentService.chooseLatest(queryDirection(query));
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        throw new UnsupportedOperationException("SingleContentService.deleteBefore is not supported");
    }

}
