package com.flightstats.hub.dao.aws;

import com.flightstats.hub.cluster.ClusterStateDao;
import com.flightstats.hub.config.ContentProperties;
import com.flightstats.hub.dao.ContentService;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.exception.NoSuchChannelException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.DirectionQuery;
import com.flightstats.hub.model.Epoch;
import com.flightstats.hub.model.SecondPath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import lombok.SneakyThrows;
import org.joda.time.DateTime;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.flightstats.hub.constant.ZookeeperNodes.HISTORICAL_EARLIEST;
import static com.flightstats.hub.constant.ZookeeperNodes.REPLICATED_LAST_UPDATED;
import static com.flightstats.hub.dao.ContentKeyUtil.enforceLimits;
import static com.flightstats.hub.dao.ContentKeyUtil.filter;

public class ContentRetriever {

    private final Dao<ChannelConfig> channelConfigDao;
    private final ClusterStateDao clusterStateDao;
    private final ContentService contentService;
    private final ContentProperties contentProperties;

    @Inject
    public ContentRetriever(@Named("ChannelConfig") Dao<ChannelConfig> channelConfigDao,
                            ClusterStateDao clusterStateDao,
                            ContentService contentService,
                            ContentProperties contentProperties) {
        this.channelConfigDao = channelConfigDao;
        this.clusterStateDao = clusterStateDao;
        this.contentService = contentService;
        this.contentProperties = contentProperties;
    }

    private DateTime getChannelTtl(ChannelConfig channelConfig, Epoch epoch) {
        DateTime ttlTime = channelConfig.getTtlTime();
        if (channelConfig.isHistorical()) {
            if (epoch.equals(Epoch.IMMUTABLE)) {
                ttlTime = channelConfig.getMutableTime().plusMillis(1);
            } else {
                final ContentKey lastKey = ContentKey.lastKey(channelConfig.getMutableTime());
                return clusterStateDao.get(channelConfig.getDisplayName(), lastKey, HISTORICAL_EARLIEST).getTime();
            }
        }
        return ttlTime;
    }


    private DirectionQuery configureStable(DirectionQuery query) {
        final ContentPath lastUpdated = getLatestLimit(query.getChannelName(), query.isStable());
        return query.withChannelStable(lastUpdated.getTime());
    }

    public boolean isReplicating(String channelName) {
        return getCachedChannelConfig(channelName)
                .filter(ChannelConfig::isReplicating)
                .isPresent();
    }

    public String getDisplayName(String channelName) {
        ChannelConfig channelConfig = getExpectedCachedChannelConfig(channelName);
        return channelConfig.getDisplayName();
    }

    @SneakyThrows
    public ChannelConfig getExpectedCachedChannelConfig(String channelName) throws NoSuchChannelException {
        return getCachedChannelConfig(channelName)
                .orElseThrow(() -> {
                    throw new NoSuchChannelException(channelName);
                });
    }

    public Optional<ChannelConfig> getCachedChannelConfig(String channelName) {
        final ChannelConfig channelConfig = channelConfigDao.getCached(channelName);
        return Optional.ofNullable(channelConfig);
    }

    public boolean isLiveChannel(String channelName) {
        return getChannelConfig(channelName, true)
                .filter(ChannelConfig::isLive)
                .isPresent();
    }

    public Optional<ChannelConfig> getChannelConfig(String channelName, boolean allowChannelCache) {
        if (allowChannelCache) {
            return getCachedChannelConfig(channelName);
        }
        return Optional.ofNullable(channelConfigDao.get(channelName));
    }

    Collection<ChannelConfig> getChannelConfig() {
        return this.channelConfigDao.getAll(false);
    }

    public boolean isExistingChannel(String channelName) {
        return channelConfigDao.exists(channelName);
    }

    public SortedSet<ContentKey> query(DirectionQuery query) {
        if (query.getCount() <= 0) {
            return Collections.emptySortedSet();
        }
        query = query.withChannelName(getDisplayName(query.getChannelName()));
        query = configureQuery(query);
        List<ContentKey> keys = new ArrayList<>(contentService.queryDirection(query));

        SortedSet<ContentKey> contentKeys = filter(keys, query);
        if (query.isInclusive()) {
            if (!contentKeys.isEmpty()) {
                if (query.isNext()) {
                    contentKeys.remove(contentKeys.last());
                } else {
                    contentKeys.remove(contentKeys.first());
                }
            }
            contentKeys.add(query.getStartKey());
        }
        ActiveTraces.getLocal().add("ChannelService.query", contentKeys);
        return contentKeys;
    }

    DirectionQuery configureQuery(DirectionQuery query) {
        ActiveTraces.getLocal().add("configureQuery.start", query);
        if (query.getCount() > contentProperties.getDirectionCountLimit()) {
            query = query.withCount(contentProperties.getDirectionCountLimit());
        }
        String channelName = query.getChannelName();
        ChannelConfig channelConfig = getExpectedCachedChannelConfig(channelName);
        query = query.withChannelConfig(channelConfig);
        DateTime ttlTime = getChannelTtl(channelConfig, query.getEpoch());
        query = query.withEarliestTime(ttlTime);

        if (query.getStartKey() == null || query.getStartKey().getTime().isBefore(ttlTime)) {
            query = query.withStartKey(new ContentKey(ttlTime, "0"));
        }
        if (query.getEpoch().equals(Epoch.MUTABLE)) {
            if (!query.isNext()) {
                DateTime mutableTime = channelConfig.getMutableTime();
                if (query.getStartKey() == null || query.getStartKey().getTime().isAfter(mutableTime)) {
                    query = query.withStartKey(new ContentKey(mutableTime.plusMillis(1), "0"));
                }
            }
        }
        query = configureStable(query);
        ActiveTraces.getLocal().add("configureQuery.end", query);
        return query;
    }


    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        if (query == null) {
            return Collections.emptySortedSet();
        }
        String channelName = query.getChannelName();
        ChannelConfig channelConfig = getCachedChannelConfig(channelName)
                .orElse(null);
        query = query.withChannelName(getDisplayName(channelName));
        query = query.withChannelConfig(channelConfig);
        ContentPath lastUpdated = getLastUpdated(query.getChannelName(), new ContentKey(TimeUtil.time(query.isStable())));
        query = query.withChannelStable(lastUpdated.getTime());
        Stream<ContentKey> stream = contentService.queryByTime(query).stream();
        stream = enforceLimits(query, stream);
        return stream.collect(Collectors.toCollection(TreeSet::new));
    }

    public Optional<ContentKey> getLatest(String channel, boolean stable) {
        channel = getDisplayName(channel);
        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .next(false)
                .stable(stable)
                .count(1)
                .build();
        return getLatest(query);
    }

    public Optional<ContentKey> getLatest(DirectionQuery query) {
        query = query.withChannelName(getDisplayName(query.getChannelName()));
        String channelName = query.getChannelName();
        if (!this.channelConfigDao.exists(channelName)) {
            return Optional.empty();
        }
        query = query.withStartKey(getLatestLimit(query.getChannelName(), query.isStable()));
        query = configureQuery(query);
        Optional<ContentKey> latest = contentService.getLatest(query);
        ActiveTraces.getLocal().add("before filter", channelName, latest);
        if (latest.isPresent()) {
            SortedSet<ContentKey> filtered = filter(latest
                    .map(Collections::singleton)
                    .orElseGet(Collections::emptySet), query);
            if (filtered.isEmpty()) {
                return Optional.empty();
            }
        }
        return latest;
    }
    public ContentKey getLatestLimit(String channelName, boolean stable) {
        DateTime time = TimeUtil.now().plusMinutes(1);
        if (stable || !isLiveChannel(channelName)) {
            time = getLastUpdated(channelName, new ContentKey(TimeUtil.stable())).getTime();
        }
        return ContentKey.lastKey(time);
    }


    public ContentPath getLastUpdated(String channelName, ContentPath defaultValue) {
        channelName = getDisplayName(channelName);
        if (isReplicating(channelName)) {
            ContentPath contentPath = clusterStateDao.get(channelName, defaultValue, REPLICATED_LAST_UPDATED);
            //REPLICATED_LAST_UPDATED is inclusive, and we want to be exclusive.
            if (!contentPath.equals(defaultValue)) {
                contentPath = new SecondPath(contentPath.getTime().plusSeconds(1));
            }
            return contentPath;
        }
        return defaultValue;
    }

}
