package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.*;
import com.flightstats.hub.dao.ContentKeyUtil;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.*;
import com.flightstats.hub.replication.S3Batch;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class SpokeS3ContentService implements ContentService {

    private final static Logger logger = LoggerFactory.getLogger(SpokeS3ContentService.class);
    private static final String CHANNEL_LATEST_UPDATED = "/ChannelLatestUpdated/";
    private final boolean dropSomeWrites = HubProperties.getProperty("s3.dropSomeWrites", false);
    private final int spokeTtlMinutes = HubProperties.getSpokeTtl();
    @Inject
    @Named(ContentDao.CACHE)
    private ContentDao spokeContentDao;
    @Inject
    @Named(ContentDao.SINGLE_LONG_TERM)
    private ContentDao s3SingleContentDao;
    @Inject
    @Named(ContentDao.BATCH_LONG_TERM)
    private ContentDao s3BatchContentDao;
    @Inject
    private ChannelService channelService;
    @Inject
    private LastContentPath lastContentPath;
    @Inject
    private S3WriteQueue s3WriteQueue;
    @Inject
    private HubUtils hubUtils;

    public SpokeS3ContentService() {
        HubServices.registerPreStop(new SpokeS3ContentServiceInit());
        HubServices.register(new ChannelLatestUpdatedService());
    }

    @Override
    public ContentKey insert(String channelName, Content content) throws Exception {
        ContentKey key = spokeContentDao.insert(channelName, content);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (channel.isSingle() || channel.isBoth()) {
            Supplier<Void> local = () -> {
                s3SingleWrite(channelName, key);
                return null;
            };
            GlobalChannelService.handleGlobal(channel, local, () -> null, local);
        }
        return key;
    }

    private void s3SingleWrite(String channelName, ContentKey key) {
        if (dropSomeWrites && Math.random() > 0.5) {
            logger.debug("dropping {} {}", channelName, key);
        } else {
            s3WriteQueue.add(new ChannelContentKey(channelName, key));
        }
    }

    @Override
    public Collection<ContentKey> insert(BulkContent bulkContent) throws Exception {
        String channelName = bulkContent.getChannel();
        SortedSet<ContentKey> keys = spokeContentDao.insert(bulkContent);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (channel.isSingle() || channel.isBoth()) {
            for (ContentKey key : keys) {
                s3SingleWrite(channelName, key);
            }
        }
        return keys;
    }

    @Override
    public boolean historicalInsert(String channelName, Content content) throws Exception {
        DateTime spokeTtlTime = TimeUtil.now().minusMinutes(spokeTtlMinutes);
        if (content.getContentKey().get().getTime().isAfter(spokeTtlTime)) {
            throw new InvalidRequestException("you cannot insert an item within the last " + spokeTtlMinutes + " minutes");
        }
        insert(channelName, content);
        return true;
    }

    @Override
    public Optional<Content> get(String channelName, ContentKey key) {
        logger.trace("fetching {} from channel {} ", key.toString(), channelName);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (channel.isHistorical() || key.getTime().isAfter(getSpokeTtlTime(channelName))) {
            Content content = spokeContentDao.get(channelName, key);
            if (content != null) {
                logger.trace("returning from spoke {} {}", key.toString(), channelName);
                return Optional.of(content);
            }
        }
        Content content;
        if (channel.isSingle()) {
            content = s3SingleContentDao.get(channelName, key);
        } else if (channel.isBatch()) {
            content = s3BatchContentDao.get(channelName, key);
        } else {
            content = s3SingleContentDao.get(channelName, key);
            if (content == null) {
                content = s3BatchContentDao.get(channelName, key);
            }
        }
        return Optional.fromNullable(content);
    }

    private DateTime getSpokeTtlTime(String channelName) {
        DateTime startTime = channelService.getLastUpdated(channelName, new ContentKey(TimeUtil.now())).getTime();
        return startTime.minusMinutes(spokeTtlMinutes);
    }

    @Override
    public void get(String channelName, SortedSet<ContentKey> keys, Consumer<Content> callback) {
        SortedSet<MinutePath> minutePaths = ContentKeyUtil.convert(keys);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        DateTime spokeTtlTime = getSpokeTtlTime(channelName);
        for (MinutePath minutePath : minutePaths) {
            if (minutePath.getTime().isAfter(spokeTtlTime)
                    || channel.isSingle()) {
                getValues(channelName, callback, minutePath);
            } else {
                if (!s3BatchContentDao.streamMinute(channelName, minutePath, callback)) {
                    getValues(channelName, callback, minutePath);
                }
            }
        }
    }

    private void getValues(String channelName, Consumer<Content> callback, ContentPathKeys contentPathKeys) {
        for (ContentKey contentKey : contentPathKeys.getKeys()) {
            Optional<Content> contentOptional = get(channelName, contentKey);
            if (contentOptional.isPresent()) {
                callback.accept(contentOptional.get());
            }
        }
    }

    @Override
    public Collection<ContentKey> queryByTime(TimeQuery query) {
        return handleQuery(query, contentDao -> contentDao.queryByTime(query));
    }

    @Override
    public Collection<ContentKey> queryDirection(DirectionQuery query) {
        return handleQuery(query, contentDao -> contentDao.query(query));
    }

    private Collection<ContentKey> handleQuery(Query query, Function<ContentDao, SortedSet<ContentKey>> daoQuery) {
        List<ContentDao> daos = new ArrayList<>();
        if (query.getLocation().equals(Location.CACHE)) {
            daos.add(spokeContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM)) {
            daos.add(s3SingleContentDao);
            daos.add(s3BatchContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM_SINGLE)) {
            daos.add(s3SingleContentDao);
        } else if (query.getLocation().equals(Location.LONG_TERM_BATCH)) {
            daos.add(s3BatchContentDao);
        } else {
            daos.add(spokeContentDao);
            ChannelConfig channel = channelService.getCachedChannelConfig(query.getChannelName());
            if (query.outsideOfCache(getSpokeTtlTime(query.getChannelName())) || channel.isHistorical()) {
                if (channel.isSingle()) {
                    daos.add(s3SingleContentDao);
                } else if (channel.isBatch()) {
                    daos.add(s3BatchContentDao);
                } else {
                    daos.add(s3SingleContentDao);
                    daos.add(s3BatchContentDao);
                }
            }
        }

        return CommonContentService.query(daoQuery, daos);
    }



    @Override
    public Optional<ContentKey> getLatest(String channel, ContentKey limitKey, Traces traces, boolean stable) {
        final ChannelConfig cachedChannelConfig = channelService.getCachedChannelConfig(channel);
        DateTime cacheTtlTime = getSpokeTtlTime(channel);
        Optional<ContentKey> latest = spokeContentDao.getLatest(channel, limitKey, traces);
        if (latest.isPresent()) {
            logger.info("found latest {} {}", channel, latest);
            lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            return latest;
        }
        ContentPath latestCache = lastContentPath.get(channel, null, CHANNEL_LATEST_UPDATED);
        if (latestCache != null) {
            DateTime channelTtlTime = cachedChannelConfig.getTtlTime();
            if(latestCache.getTime().isBefore(channelTtlTime)){
                lastContentPath.update(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            }
            logger.info("found cached {} {}", channel, latestCache);
            if (latestCache.equals(ContentKey.NONE)) {
                return Optional.absent();
            }
            return Optional.of((ContentKey) latestCache);
        }

        DirectionQuery query = DirectionQuery.builder()
                .channelName(channel)
                .contentKey(limitKey)
                .next(false)
                .stable(stable)
                .count(1)
                .build();
        Collection<ContentKey> keys = channelService.getKeys(query);
        if (keys.isEmpty()) {
            logger.debug("updating channel empty {}", channel);
            lastContentPath.updateIncrease(ContentKey.NONE, channel, CHANNEL_LATEST_UPDATED);
            return Optional.absent();
        } else {
            ContentKey latestKey = keys.iterator().next();
            if (latestKey.getTime().isAfter(cacheTtlTime)) {
                logger.debug("latestKey within spoke window {} {}", channel, latestKey);
                lastContentPath.delete(channel, CHANNEL_LATEST_UPDATED);
            } else {
                logger.debug("updating cache with latestKey {} {}", channel, latestKey);
                lastContentPath.update(latestKey, channel, CHANNEL_LATEST_UPDATED);
            }
            return Optional.of(latestKey);
        }
    }

    @Override
    public void delete(String channelName) {
        logger.info("deleting channel " + channelName);
        spokeContentDao.delete(channelName);
        s3SingleContentDao.delete(channelName);
        s3BatchContentDao.delete(channelName);
        lastContentPath.delete(channelName, CHANNEL_LATEST_UPDATED);
        lastContentPath.delete(channelName, S3Verifier.LAST_SINGLE_VERIFIED);
        ChannelConfig channel = channelService.getCachedChannelConfig(channelName);
        if (!channel.isSingle()) {
            new S3Batch(channel, hubUtils).stop();
        }
    }

    @Override
    public void deleteBefore(String name, ContentKey limitKey) {
        s3SingleContentDao.deleteBefore(name, limitKey);
        s3BatchContentDao.deleteBefore(name, limitKey);
    }

    @Override
    public void notify(ChannelConfig newConfig, ChannelConfig oldConfig) {
        if (oldConfig == null) {
            lastContentPath.updateIncrease(ContentKey.NONE, newConfig.getName(), CHANNEL_LATEST_UPDATED);
        }
        if (newConfig.isSingle()) {
            if (oldConfig != null && !oldConfig.isSingle()) {
                new S3Batch(newConfig, hubUtils).stop();
            }
        } else {
            new S3Batch(newConfig, hubUtils).start();
        }
    }

    private class SpokeS3ContentServiceInit extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            spokeContentDao.initialize();
            s3SingleContentDao.initialize();
            s3BatchContentDao.initialize();
        }

        @Override
        protected void shutDown() throws Exception {
            //do nothing
        }
    }

    private class ChannelLatestUpdatedService extends AbstractScheduledService {

        @Override
        protected synchronized void runOneIteration() throws Exception {
            logger.debug("running...");
            channelService.getChannels().forEach(channelConfig -> {
                try {
                    DateTime time = TimeUtil.stable().plusMinutes(1);
                    Traces traces = new Traces(channelConfig.getName(), time);
                    Optional<ContentKey> latest = getLatest(channelConfig.getName(), ContentKey.lastKey(time), traces, false);
                    logger.debug("latest updated {} {}", channelConfig.getName(), latest);
                    traces.log(logger);
                } catch (Exception e) {
                    logger.warn("unexpected ChannelLatestUpdatedService issue " + channelConfig.getName(), e);
                }
            });
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 59, TimeUnit.MINUTES);
        }

    }

}
