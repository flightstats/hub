package com.flightstats.hub.spoke;

import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.exception.ContentTooLargeException;
import com.flightstats.hub.exception.FailedWriteException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.BulkContent;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.util.Commander;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;

import static com.flightstats.hub.constant.ContentConstant.GET_ITEM_COUNT_COMMAND;
import static com.flightstats.hub.constant.ContentConstant.GET_OLDEST_ITEM_COMMAND;

@Slf4j
public class SpokeContentDao {

    private final Commander commander;
    private final SpokeProperties spokeProperties;

    @Inject
    public SpokeContentDao(Commander commander, SpokeProperties spokeProperties) {
        this.commander = commander;
        this.spokeProperties = spokeProperties;
    }

    @SneakyThrows
    public static SortedSet<ContentKey> insert(BulkContent bulkContent, Function<ByteArrayOutputStream, Boolean> inserter) {
        Traces traces = ActiveTraces.getLocal();
        traces.add("writeBulk");
        String channelName = bulkContent.getChannel();
        try {
            SortedSet<ContentKey> keys = new TreeSet<>();
            List<Content> items = bulkContent.getItems();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(baos);
            stream.writeInt(items.size());
            log.debug("writing {} items to master {}", items.size(), bulkContent.getMasterKey());
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

            log.trace("writing items {} to channel {}", items.size(), channelName);
            if (!inserter.apply(baos)) {
                throw new FailedWriteException("unable to write bulk to spoke " + channelName);
            }
            traces.add("writeBulk completed", keys);
            return keys;
        } catch (ContentTooLargeException e) {
            log.info("content too large for channel " + channelName);
            throw e;
        } catch (Exception e) {
            traces.add("SpokeContentDao", "error", e.getMessage());
            log.error("unable to write " + channelName, e);
            throw e;
        }
    }

    Optional<ChannelContentKey> getOldestItem(SpokeStore store) {
        String storePath = spokeProperties.getPath(store);
        log.trace("getting oldest item from " + storePath);
        // expected result format: YYYY-MM-DD+HH:MM:SS.SSSSSSSSSS /mnt/spoke/store/channel/yyyy/mm/dd/hh/mm/ssSSShash
        String command = String.format(GET_OLDEST_ITEM_COMMAND, storePath);
        int waitTimeSeconds = 3;
        String result = StringUtils.chomp(commander.runInBash(command, waitTimeSeconds));
        if (StringUtils.isEmpty(result)) return Optional.empty();
        try {
            return Optional.of(ChannelContentKey.fromSpokePath(result));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    long getNumberOfItems(SpokeStore spokeStore) {
        String storePath = spokeProperties.getPath(spokeStore);
        log.trace("getting the total number of items in " + storePath);
        String command = String.format(GET_ITEM_COUNT_COMMAND, storePath);
        int waitTimeSeconds = 1;
        String result = StringUtils.chomp(commander.runInBash(command, waitTimeSeconds));
        return StringUtils.isEmpty(result) ? 0L : Long.parseLong(result);
    }

}
