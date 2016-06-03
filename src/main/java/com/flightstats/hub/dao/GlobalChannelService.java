package com.flightstats.hub.dao;

import com.diffplug.common.base.Errors;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.Replicator;
import com.flightstats.hub.util.HubUtils;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.joda.time.DateTime;

import java.util.Collection;
import java.util.SortedSet;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The GlobalChannelService is a pass through for non-global channels
 * For Global channels, it can take different paths ...
 */
@Singleton
public class GlobalChannelService implements ChannelService {

    private HubUtils hubUtils;
    private LocalChannelService localChannelService;
    @Inject
    @Named(ContentDao.CACHE)
    private ContentDao spokeContentDao;
    private static final LastContentPath lastReplicated = HubProvider.getInstance(LastContentPath.class);

    //todo - gfm - 6/2/16 - figure out where this should live
    private final int spokeTtlMinutes = HubProperties.getSpokeTtl();

    @Inject
    public GlobalChannelService(LocalChannelService localChannelService, HubUtils hubUtils) {
        this.localChannelService = localChannelService;
        this.hubUtils = hubUtils;
    }

    public static <X> X handleGlobal(ChannelConfig channel, Supplier<X> local, Supplier<X> satellite, Supplier<X> master) {
        if (channel.isGlobal()) {
            if (channel.isGlobalMaster()) {
                return master.get();
            } else {
                return satellite.get();
            }
        } else {
            return local.get();
        }
    }

    private <X> X handleGlobal(String channelName, Supplier<X> local, Supplier<X> satellite) {
        return handleGlobal(channelName, local, satellite, local);
    }

    private <X> X handleGlobal(String channelName, Supplier<X> local, Supplier<X> satellite, Supplier<X> master) {
        return handleGlobal(getCachedChannelConfig(channelName), local, satellite, master);
    }

    @Override
    public boolean channelExists(String channelName) {
        return false;
    }

    @Override
    public ChannelConfig createChannel(ChannelConfig channel) {
        Supplier<ChannelConfig> local = () -> localChannelService.createChannel(channel);
        Supplier<ChannelConfig> global = createGlobalMaster(channel);
        return handleGlobal(channel, local, global, global);
    }

    @Override
    public ChannelConfig updateChannel(ChannelConfig channel, ChannelConfig oldConfig) {
        Supplier<ChannelConfig> local = () -> localChannelService.updateChannel(channel, oldConfig);
        Supplier<ChannelConfig> global = createGlobalMaster(channel);
        return handleGlobal(channel, local, global, global);
    }

    private Supplier<ChannelConfig> createGlobalMaster(ChannelConfig channel) {
        return () -> {
            if (hubUtils.putChannel(channel.getGlobal().getMaster() + "internal/global/master/" + channel.getName(), channel)) {
                return channel;
            }
            return null;
        };
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        Supplier<ContentKey> local = Errors.rethrow().wrap(() -> {
            return localChannelService.insert(channelName, content);
        });
        Supplier<ContentKey> satellite = () -> hubUtils.insert(getMasterChannelUrl(channelName), content);
        return handleGlobal(channelName, local, satellite);
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulk) throws Exception {
        Supplier<Collection<ContentKey>> local = Errors.rethrow().wrap(() -> {
            return localChannelService.insert(bulk);
        });
        Supplier<Collection<ContentKey>> satellite = () -> hubUtils.insert(getMasterChannelUrl(bulk.getChannel()), bulk);
        return handleGlobal(bulk.getChannel(), local, satellite);
    }

    @Override
    public boolean isReplicating(String channelName) {
        return localChannelService.isReplicating(channelName);
    }

    @Override
    public Optional<ContentKey> getLatest(String channelName, boolean stable, boolean trace) {
        Supplier<Optional<ContentKey>> local = () -> localChannelService.getLatest(channelName, stable, trace);
        Supplier<Optional<ContentKey>> satellite = () -> {
            ContentKey limitKey = LocalChannelService.getLatestLimit(stable);
            Optional<ContentKey> latest = spokeContentDao.getLatest(channelName, limitKey, ActiveTraces.getLocal());
            if (latest.isPresent()) {
                return latest;
            }
            Optional<String> fullKey = hubUtils.getLatest(getMasterChannelUrl(channelName));
            if (fullKey.isPresent()) {
                return Optional.fromNullable(ContentKey.fromFullUrl(fullKey.get()));
            }
            return Optional.absent();
        };
        return handleGlobal(channelName, local, satellite);
    }

    private String getMasterChannelUrl(String channelName) {
        ChannelConfig config = localChannelService.getCachedChannelConfig(channelName);
        return config.getGlobal().getMaster() + "/channel/" + channelName;
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        Supplier<Void> local = () -> {
            localChannelService.deleteBefore(name, limitKey);
            return null;
        };
        Supplier<Void> satellite = () -> null;
        handleGlobal(name, local, satellite);
    }

    @Override
    public Optional<Content> getValue(Request request) {
        Supplier<Optional<Content>> local = () -> localChannelService.getValue(request);
        Supplier<Optional<Content>> satellite = () -> {
            Content read = spokeContentDao.read(request.getChannel(), request.getKey());
            if (read != null) {
                return Optional.of(read);
            }
            return Optional.fromNullable(hubUtils.get(getMasterChannelUrl(request.getChannel()), request.getKey()));
        };
        return handleGlobal(request.getChannel(), local, satellite);
    }

    @Override
    public Iterable<String> getTags() {
        return localChannelService.getTags();
    }

    @Override
    public SortedSet<ContentKey> queryByTime(TimeQuery query) {
        Supplier<SortedSet<ContentKey>> local = () -> localChannelService.queryByTime(query);
        Supplier<SortedSet<ContentKey>> satellite = () -> query(query, spokeContentDao.queryByTime(query));
        return handleGlobal(query.getChannelName(), local, satellite);
    }

    @Override
    public SortedSet<ContentKey> getKeys(DirectionQuery query) {
        Supplier<SortedSet<ContentKey>> local = () -> localChannelService.getKeys(query);
        Supplier<SortedSet<ContentKey>> satellite = () -> query(query, spokeContentDao.query(query));
        return handleGlobal(query.getChannelName(), local, satellite);
    }

    private SortedSet<ContentKey> query(Query query, SortedSet<ContentKey> contentKeys) {
        if (query.outsideOfCache(getSpokeCacheTime(query))) {
            contentKeys.addAll(hubUtils.query(getMasterChannelUrl(query.getChannelName()), query));
        }
        return contentKeys;
    }

    private DateTime getSpokeCacheTime(Query query) {
        DateTime startTime = lastReplicated.get(query.getChannelName(), MinutePath.NONE, Replicator.REPLICATED_LAST_UPDATED).getTime();
        startTime.minusMinutes(spokeTtlMinutes);
        return startTime;
    }

    @Override
    public void getValues(String channel, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        //todo - gfm - 5/19/16 - what does this do again?
    }

    @Override
    public boolean delete(String channelName) {
        Supplier<Boolean> local = () -> localChannelService.delete(channelName);
        Supplier<Boolean> satellite = () -> hubUtils.delete(getMasterChannelUrl(channelName));
        return handleGlobal(channelName, local, satellite);
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
    public Iterable<ChannelConfig> getChannels() {
        return localChannelService.getChannels();
    }

    @Override
    public Iterable<ChannelConfig> getChannels(String tag) {
        return localChannelService.getChannels(tag);
    }

}
