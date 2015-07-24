package com.flightstats.hub.dao.aws;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.Traces;
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

@SuppressWarnings("Convert2Lambda")
public class S3WriterManager {
    private final static Logger logger = LoggerFactory.getLogger(S3WriterManager.class);

    private final ChannelService channelService;
    private final ContentDao cacheContentDao;
    private final ContentDao longTermContentDao;
    private final S3WriteQueue s3WriteQueue;
    private final int offsetMinutes;
    private final ExecutorService queryThreadPool;
    private final ExecutorService channelThreadPool;

    @Inject
    public S3WriterManager(ChannelService channelService,
                           @Named(ContentDao.CACHE) ContentDao cacheContentDao,
                           @Named(ContentDao.LONG_TERM) ContentDao longTermContentDao,
                           S3WriteQueue s3WriteQueue) {
        this.channelService = channelService;
        this.cacheContentDao = cacheContentDao;
        this.longTermContentDao = longTermContentDao;
        this.s3WriteQueue = s3WriteQueue;
        HubServices.register(new S3WriterManagerService(), HubServices.TYPE.POST_START, HubServices.TYPE.PRE_STOP);

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

    SortedSet<ContentKey> itemsInCacheButNotLongTerm(DateTime startTime, String channelName) {
        SortedSet<ContentKey> cacheKeys = new TreeSet<>();
        SortedSet<ContentKey> longTermKeys = new TreeSet<>();
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            queryThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    cacheKeys.addAll(cacheContentDao.queryByTime(channelName, startTime, TimeUtil.Unit.MINUTES,
                            Traces.NOOP));
                    countDownLatch.countDown();
                }
            });
            queryThreadPool.submit(new Runnable() {
                @Override
                public void run() {
                    longTermKeys.addAll(longTermContentDao.queryByTime(channelName, startTime, TimeUtil.Unit.MINUTES,
                            Traces.NOOP));
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await(3, TimeUnit.MINUTES);
            cacheKeys.removeAll(longTermKeys);
            if (cacheKeys.size() > 0) {
                logger.info("missing {} items in channel {}", cacheKeys.size(), channelName);
                logger.info("channel {} missing items {}", channelName, cacheKeys);
            }
            return cacheKeys;
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }

    }

    public void run() {
        try {
            DateTime startTime = DateTime.now().minusMinutes(offsetMinutes);
            logger.info("Verifying S3 data at: {}", startTime);
            Iterable<ChannelConfig> channels = channelService.getChannels();
            for (ChannelConfig channel : channels) {
                channelThreadPool.submit(new Runnable() {
                    @Override
                    public void run() {
                        String channelName = channel.getName();
                        SortedSet<ContentKey> keysToAdd = itemsInCacheButNotLongTerm(startTime, channelName);
                        for (ContentKey key : keysToAdd) {
                            s3WriteQueue.add(new ChannelContentKey(channelName, key));
                        }
                    }
                });
            }
        } catch (Exception e) {
            logger.error("Error: ", e);
        }
    }

    private class S3WriterManagerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
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
