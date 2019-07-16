package com.flightstats.hub.dao.aws;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;

@Slf4j
@Singleton
public class MaxItemsEnforcer {

    private final ContentRetriever contentRetriever;
    private final ChannelService channelService;
    private final Dao<ChannelConfig> channelConfigDao;

    @Inject
    public MaxItemsEnforcer(ContentRetriever contentRetriever,
                            ChannelService channelService,
                            @Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao) {
        this.contentRetriever = contentRetriever;
        this.channelService = channelService;
        this.channelConfigDao = channelConfigDao;
    }

    void updateMaxItems(Iterable<ChannelConfig> configurations) {
        log.info("iterating channels for max items update");
        for (ChannelConfig config : configurations) {
            if (config.getMaxItems() > 0 && !config.getKeepForever()) {
                updateMaxItems(config);
            }
        }
    }

    @SneakyThrows
    void updateMaxItems(String channelName) {
        Optional<ChannelConfig> channelConfig = Optional.ofNullable(channelConfigDao.get(channelName));
        channelConfig.filter(config -> config.getMaxItems() > 0 && !config.getKeepForever())
                .ifPresent(this::updateMaxItems);
    }

    private void updateMaxItems(ChannelConfig config) {
        String name = config.getDisplayName();
        log.info("updating max items for channel {}", name);
        ActiveTraces.start("S3Config.updateMaxItems", name);
        Optional<ContentKey> optional = contentRetriever.getLatest(name, false);
        if (optional.isPresent()) {
            ContentKey latest = optional.get();
            if (latest.getTime().isAfter(TimeUtil.now().minusDays(1))) {
                updateMaxItems(config, latest);
            }
        }
        ActiveTraces.end();
        log.info("completed max items for channel {}", name);

    }

    private void updateMaxItems(ChannelConfig config, ContentKey latest) {
        SortedSet<ContentKey> keys = new TreeSet<>();
        keys.add(latest);
        String name = config.getDisplayName();
        DirectionQuery query = DirectionQuery.builder()
                .channelName(name)
                .startKey(latest)
                .next(false)
                .stable(false)
                .earliestTime(config.getTtlTime())
                .count((int) (config.getMaxItems() - 1))
                .build();
        keys.addAll(contentRetriever.query(query));
        if (keys.size() == config.getMaxItems()) {
            ContentKey limitKey = keys.first();
            log.info("deleting keys before {}", limitKey);
            channelService.deleteBefore(name, limitKey);
        }
    }

}
