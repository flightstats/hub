package com.flightstats.hub.spoke;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.FailedWriteException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.Commander;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

public class SpokeContentDao {
    private static final Logger logger = LoggerFactory.getLogger(SpokeContentDao.class);

    static final String GET_OLDEST_ITEM_COMMAND = "find %s -type f -printf '%%T+ %%p\\n' | sort | head -n 1";
    static final String GET_ITEM_COUNT_COMMAND = "find %s -type f | wc -l";

    private final Commander commander;

    @Inject
    public SpokeContentDao(Commander commander) {
        this.commander = commander;
    }

    public static SortedSet<ContentKey> insert(BulkContent bulkContent, Function<ByteArrayOutputStream, Boolean> inserter) throws Exception {
        Traces traces = ActiveTraces.getLocal();
        traces.add("writeBulk");
        String channelName = bulkContent.getChannel();
        try {
            SortedSet<ContentKey> keys = new TreeSet<>();
            List<Content> items = bulkContent.getItems();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(baos);
            stream.writeInt(items.size());
            logger.debug("writing {} items to master {}", items.size(), bulkContent.getMasterKey());
            for (Content content : items) {
                content.packageStream();
                String itemKey = content.getContentKey().get().toUrl();
                stream.writeInt(itemKey.length());
                stream.write(itemKey.getBytes());
                stream.writeInt(content.getData().length);
                stream.write(content.getData());
                keys.add(content.getContentKey().get());
            }
            stream.flush();
            traces.add("writeBulk marshalled");

            logger.trace("writing items {} to channel {}", items.size(), channelName);
            if (!inserter.apply(baos)) {
                throw new FailedWriteException("unable to write bulk to spoke " + channelName);
            }
            traces.add("writeBulk completed", keys);
            return keys;
        } catch (ContentTooLargeException e) {
            logger.info("content too large for channel " + channelName);
            throw e;
        } catch (Exception e) {
            traces.add("SpokeContentDao", "error", e.getMessage());
            logger.error("unable to write " + channelName, e);
            throw e;
        }
    }

    Optional<ChannelContentKey> getOldestItem(SpokeStore store) {
        String storePath = HubProperties.getSpokePath(store);
        logger.trace("getting oldest item from " + storePath);
        // expected result format: YYYY-MM-DD+HH:MM:SS.SSSSSSSSSS /mnt/spoke/store/channel/yyyy/mm/dd/hh/mm/ssSSShash
        String command = String.format(GET_OLDEST_ITEM_COMMAND, storePath);
        String result = StringUtils.chomp(commander.runInBash(command, 3));
        if (StringUtils.isEmpty(result)) return Optional.absent();
        String spokePath = StringUtils.substring(result, 31);
        try {
            return Optional.of(ChannelContentKey.fromSpokePath(spokePath));
        } catch (IllegalArgumentException e) {
            return Optional.absent();
        }
    }

    long getNumberOfItems(SpokeStore spokeStore) {
        String storePath = HubProperties.getSpokePath(spokeStore);
        logger.trace("getting the total number of items in " + storePath);
        String command = String.format(GET_ITEM_COUNT_COMMAND, storePath);
        String result = StringUtils.chomp(commander.runInBash(command, 1));
        return StringUtils.isEmpty(result) ? 0L : Long.valueOf(result);
    }

}
