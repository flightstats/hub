package com.flightstats.hub.dao.aws;

import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.util.TimeUtil;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.StreamSupport;

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
        StreamSupport.stream(configurations.spliterator(), false)
                .filter(config -> config.getMaxItems() > 0 && !config.getKeepForever())
                .forEach(this::updateMaxItems);
    }

    void updateMaxItems(String channelName) {
        Optional<ChannelConfig> channelConfig = Optional.ofNullable(channelConfigDao.get(channelName));
        channelConfig.filter(config -> config.getMaxItems() > 0 && !config.getKeepForever())
                .ifPresent(this::updateMaxItems);
    }

    private void updateMaxItems(ChannelConfig config) {
        String name = config.getDisplayName();
        log.debug("updating max items for channel {}", name);
        ActiveTraces.start("S3Config.updateMaxItems", name);
        Optional<ContentKey> optionalLatest = contentRetriever.getLatest(name, false);
        optionalLatest.filter(latest -> latest.getTime().isAfter(TimeUtil.now().minusDays(1)))
                .ifPresent(latest -> updateMaxItems(config, latest));
        ActiveTraces.end();
        log.info("completed max items update for channel {}. updated: {}", name, optionalLatest.map(ContentKey::toUrl).orElse("None"));
    }

    private Optional<ContentKey> getEarliestKeyOfMaximum(ChannelConfig config, ContentKey latest) {
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
        if (keys.size() != config.getMaxItems()) {
            return Optional.empty();
        }
        return Optional.of(keys.first());
    }

    private void updateMaxItems(ChannelConfig config, ContentKey latest) {
        Optional<ContentKey> limitKey = getEarliestKeyOfMaximum(config, latest);
        limitKey.ifPresent(key -> {
            log.info("deleting keys before {}", key);
            channelService.deleteBefore(config.getDisplayName(), key);
        });
    }

}
