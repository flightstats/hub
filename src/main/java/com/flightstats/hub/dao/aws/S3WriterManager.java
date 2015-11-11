package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.group.MinuteGroupStrategy;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class S3WriterManager {
    private final static Logger logger = LoggerFactory.getLogger(S3WriterManager.class);
    public static final String APP_URL = HubProperties.getAppUrl();

    private final ChannelService channelService;
    private final ContentDao spokeContentDao;
    private final ContentDao s3SingleContentDao;
    private final ContentDao s3BatchContentDao;
    private final S3WriteQueue s3WriteQueue;
    private final int offsetMinutes;
    private final ExecutorService queryThreadPool;
    private final ExecutorService channelThreadPool;

    @Inject
    public S3WriterManager(ChannelService channelService,
                           @Named(ContentDao.CACHE) ContentDao spokeContentDao,
                           @Named(ContentDao.SINGLE_LONG_TERM) ContentDao s3SingleContentDao,
                           @Named(ContentDao.BATCH_LONG_TERM) ContentDao s3BatchContentDao,
                           S3WriteQueue s3WriteQueue) {
        this.channelService = channelService;
        this.spokeContentDao = spokeContentDao;
        this.s3SingleContentDao = s3SingleContentDao;
        this.s3BatchContentDao = s3BatchContentDao;
        this.s3WriteQueue = s3WriteQueue;
        HubServices.register(new S3WriterManagerSingleService(), HubServices.TYPE.FINAL_POST_START, HubServices.TYPE.PRE_STOP);
        HubServices.register(new S3WriterManagerBatchService(), HubServices.TYPE.FINAL_POST_START, HubServices.TYPE.PRE_STOP);

        this.offsetMinutes = serverOffset();
        queryThreadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("S3QueryThread-%d")
                .build());
        channelThreadPool = Executors.newFixedThreadPool(10, new ThreadFactoryBuilder().setNameFormat("S3ChannelThread-%d")
                .build());
    }

    static int serverOffset() {
        String host = HubHost.getLocalName();
        int ttlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);
        int shiftMinutes = 5;
        int randomOffset = shiftMinutes + (int) (Math.random() * (ttlMinutes - shiftMinutes * 3));
        int offset = HubProperties.getProperty("s3.verifyOffset." + host, randomOffset);
        logger.info("{} offset is -{} minutes", host, offset);
        return offset;
    }

    SortedSet<ContentKey> getMissing(DateTime startTime, DateTime endTime, String channelName, ContentDao s3ContentDao,
                                     SortedSet<ContentKey> expectedKeys) {
        SortedSet<ContentKey> cacheKeys = new TreeSet<>();
        SortedSet<ContentKey> longTermKeys = new TreeSet<>();
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .endTime(endTime)
                .unit(TimeUtil.Unit.MINUTES)
                .traces(Traces.NOOP)
                .build();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            queryThreadPool.submit(() -> {
                SortedSet<ContentKey> spokeKeys = spokeContentDao.queryByTime(timeQuery);
                cacheKeys.addAll(spokeKeys);
                expectedKeys.addAll(spokeKeys);
                countDownLatch.countDown();
            });
            queryThreadPool.submit(() -> {
                longTermKeys.addAll(s3ContentDao.queryByTime(timeQuery));
                countDownLatch.countDown();
            });
            countDownLatch.await(3, TimeUnit.MINUTES);
            cacheKeys.removeAll(longTermKeys);
            if (cacheKeys.size() > 0) {
                logger.info("missing {} items in channel {}", cacheKeys.size(), channelName);
                logger.debug("channel {} missing items {}", channelName, cacheKeys);
            }
            return cacheKeys;
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    private void singleS3Verification(final DateTime startTime, final ChannelConfig channel, DateTime endTime) {
        channelThreadPool.submit(() -> {
            String channelName = channel.getName();
            SortedSet<ContentKey> keysToAdd = getMissing(startTime, endTime, channelName, s3SingleContentDao, new TreeSet<>());
            for (ContentKey key : keysToAdd) {
                s3WriteQueue.add(new ChannelContentKey(channelName, key));
            }
        });
    }

    private void batchS3Verification(final DateTime startTime, final ChannelConfig channel) {
        channelThreadPool.submit(() -> {
            String channelName = channel.getName();
            SortedSet<ContentKey> expectedKeys = new TreeSet<>();
            SortedSet<ContentKey> keysToAdd = getMissing(startTime, null, channelName, s3BatchContentDao, expectedKeys);
            if (!keysToAdd.isEmpty()) {
                MinutePath path = new MinutePath(startTime);
                logger.info("s3 batch missing {}", path);
                String batchUrl = MinuteGroupStrategy.getBatchUrl(APP_URL + "/channel/" + channelName, path);
                S3BatchResource.getAndWriteBatch(s3BatchContentDao, channelName, path, expectedKeys, batchUrl);
            }
        });
    }

    public void runSingle() {
        try {
            DateTime startTime = DateTime.now().minusMinutes(offsetMinutes);
            DateTime endTime = DateTime.now().minusMinutes(offsetMinutes).plusMinutes(20);
            logger.info("Verifying Single S3 data at: {}", startTime);
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isSingle() || channel.isBoth()) {
                    singleS3Verification(startTime, channel, endTime);
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    public void runBatch() {
        try {
            DateTime startTime = DateTime.now().minusMinutes(offsetMinutes);
            logger.info("Verifying Batch S3 data at: {}", startTime);
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                if (channel.isBatch() || channel.isBoth()) {
                    batchS3Verification(startTime, channel);
                }
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    private class S3WriterManagerSingleService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            runSingle();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 15, TimeUnit.MINUTES);
        }

        @Override
        protected void shutDown() throws Exception {
            s3WriteQueue.close();
        }
    }

    private class S3WriterManagerBatchService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            runBatch();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.MINUTES);
        }

        @Override
        protected void shutDown() throws Exception {
            s3WriteQueue.close();
        }
    }
}
