package com.flightstats.hub.dao;

import com.diffplug.common.base.Errors;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.spoke.SpokeStore;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Supplier;

import static java.util.Objects.isNull;

/**
 * The GlobalChannelService is a pass through for standard channels
 * For Global channels, it can take different paths depending on whether this cluster is the Master or a Satellite.
 */
@Singleton
public class GlobalChannelService implements ChannelService {

    @Inject
    private HubUtils hubUtils;
    @Inject
    private LocalChannelService localChannelService;
    @Inject
    @Named(ContentDao.WRITE_CACHE)
    private ContentDao spokeWriteContentDao;

    public static <X> X handleGlobal(ChannelConfig channel, Supplier<X> standard, Supplier<X> satellite, Supplier<X> master) {
        if (channel.isGlobal()) {
            if (channel.isGlobalMaster()) {
                return master.get();
            } else {
                return satellite.get();
            }
        } else {
            return standard.get();
        }
    }

    /**
     * Handle the standard differently from global channels.
     */
    private <X> X standardAndGlobal(ChannelConfig channel, Supplier<X> standard, Supplier<X> global) {
        return handleGlobal(channel, standard, global, global);
    }

    /**
     * Handle the primary sources (standard and global master differently from the secondary source (satellite).
     */
    private <X> X primaryAndSecondary(String channelName, Supplier<X> primary, Supplier<X> secondary) {
        return handleGlobal(getCachedChannelConfig(channelName), primary, secondary, primary);
    }

    @Override
    public boolean channelExists(String channelName) {
        return localChannelService.channelExists(channelName);
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig channel) {
        return standardAndGlobal(channel,
                () -> localChannelService.createChannel(channel),
                createGlobalMaster(channel));
    }

    @Override
    public ChannelConfig updateChannel(ChannelConfig channel, ChannelConfig oldConfig, boolean isLocalHost) {
        return standardAndGlobal(channel,
                () -> localChannelService.updateChannel(channel, oldConfig, isLocalHost),
                createGlobalMaster(channel));
    }

    private Supplier<ChannelConfig> createGlobalMaster(ChannelConfig channel) {
        return () -> {
            if (hubUtils.putChannel(channel.getGlobal().getMaster() + "internal/global/master/" + channel.getDisplayName(), channel)) {
                return channel;
            }
            return null;
        };
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        return primaryAndSecondary(channelName,
                Errors.rethrow().wrap(() -> localChannelService.insert(channelName, content)),
                () -> hubUtils.insert(getMasterChannelUrl(channelName), content));
    }

    @Override
    public boolean historicalInsert(String channelName, Content content) {
        return primaryAndSecondary(channelName,
                Errors.rethrow().wrap(() -> localChannelService.historicalInsert(channelName, content)),
                () -> {
                    ContentKey key = hubUtils.insert(getHistoricalInsertUrl(getMasterChannelUrl(channelName), content), content);
                    return !isNull(key);
                });
    }

    private String getHistoricalInsertUrl(String masterChannelUrl, Content content) {
        return masterChannelUrl + "/" + TimeUtil.millis(content.getContentKey().get().getTime());
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulk) throws Exception {
        return primaryAndSecondary(bulk.getChannel(),
                Errors.rethrow().wrap(() -> localChannelService.insert(bulk)),
                () -> hubUtils.insert(getMasterChannelUrl(bulk.getChannel()), bulk));
    }

    @Override
    public boolean isReplicating(String channelName) {
        return localChannelService.isReplicating(channelName);
    }

    @Override
    public Optional<ContentKey> getLatest(DirectionQuery query) {
        String channelName = query.getChannelName();
        return primaryAndSecondary(channelName,
                () -> localChannelService.getLatest(query),
                () -> {
                    ContentKey limitKey = localChannelService.getLatestLimit(channelName, query.isStable());
                    Optional<ContentKey> latest = spokeWriteContentDao.getLatest(channelName, limitKey, ActiveTraces.getLocal());
                    if (latest.isPresent()) {
                        return latest;
                    }
                    Optional<String> fullKey = hubUtils.getLatest(getMasterChannelUrl(channelName));
                    ContentKey contentKey = null;
                    if (fullKey.isPresent()) {
                        contentKey = ContentKey.fromFullUrl(fullKey.get());
                    }
                    return Optional.fromNullable(contentKey);
                });
    }

    private String getMasterChannelUrl(String channelName) {
        return localChannelService.getCachedChannelConfig(channelName).getGlobal().getMaster() + "channel/" + channelName;
    }

    @Override
    public void deleteBefore(String channel, ContentKey limitKey) {
        primaryAndSecondary(channel,
                () -> {
                    localChannelService.deleteBefore(channel, limitKey);
                    return null;
                },
                (Supplier<Void>) () -> null);
    }

    @Override
    public Optional<Content> get(ItemRequest itemRequest) {
        return primaryAndSecondary(itemRequest.getChannel(),
                () -> localChannelService.get(itemRequest),
                () -> {
                    Content read = spokeWriteContentDao.get(itemRequest.getChannel(), itemRequest.getKey());
                    if (read != null) {
                        return Optional.of(read);
                    }
                    return Optional.fromNullable(hubUtils.get(getMasterChannelUrl(itemRequest.getChannel()), itemRequest.getKey()));
                });
    }

    @Override
    public Iterable<String> getTags() {
        return localChannelService.getTags();
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        return primaryAndSecondary(query.getChannelName(),
                () -> localChannelService.queryByTime(query),
                () -> query(query, localChannelService.queryByTime(query.withLocation(Location.CACHE_WRITE))));
    }

    @Override
    public SortedSet<ContentKey> query(DirectionQuery query) {
        return primaryAndSecondary(query.getChannelName(),
                () -> localChannelService.query(query),
                () -> query(query, localChannelService.query(query.withLocation(Location.CACHE_WRITE))));
    }

    private SortedSet<ContentKey> query(Query query, SortedSet<ContentKey> contentKeys) {
        if (query.outsideOfCache(getSpokeCacheTime(query))) {
            contentKeys.addAll(hubUtils.query(getMasterChannelUrl(query.getChannelName()), query));
        }
        return contentKeys;
    }

    private DateTime getSpokeCacheTime(Query query) {
        DateTime startTime = getLastUpdated(query.getChannelName(), MinutePath.NONE).getTime();
        return startTime.minusMinutes(HubProperties.getSpokeTtlMinutes(SpokeStore.WRITE));
    }

    @Override
    public void get(StreamResults streamResults) {
        primaryAndSecondary(streamResults.getChannel(),
                () -> {
                    localChannelService.get(streamResults);
                    return null;
                },
                () -> {
                    //todo - gfm - 6/3/16 - if this is outside of the spoke TTL window, call the master.
                    localChannelService.get(streamResults);
                    return null;
                });
    }

    @Override
    public boolean delete(String channelName) {
        return localChannelService.delete(channelName);
    }

    @Override
    public boolean delete(String channelName, ContentKey contentKey) {
        return localChannelService.delete(channelName, contentKey);
    }

    @Override
    public ContentPath getLastUpdated(String channelName, ContentPath defaultValue) {
        return localChannelService.getLastUpdated(channelName, defaultValue);
    }

    @Override
    public ChannelConfig getChannelConfig(String channelName, boolean allowChannelCache) {
        return localChannelService.getChannelConfig(channelName, allowChannelCache);
    }

    @Override
    public ChannelConfig getCachedChannelConfig(String channelName) {
        return localChannelService.getCachedChannelConfig(channelName);
    }

    @Override
    public Collection<ChannelConfig> getChannels() {
        return localChannelService.getChannels();
    }

    @Override
    public Collection<ChannelConfig> getChannels(String tag, boolean useCache) {
        return localChannelService.getChannels(tag, useCache);
    }

}
