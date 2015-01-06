package com.flightstats.hub.dao.s3;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.*;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.joda.time.DateTime;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("Convert2Lambda")
public class S3WriterManager {

    private final ChannelService channelService;
    private final ContentDao cacheContentDao;
    private final ContentDao longTermContentDao;
    private final S3WriteQueue s3WriteQueue;
    private final int offsetMinutes;
    private final ExecutorService executorService;

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

        String host="";
        try {
            host = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        if(host.contains("1.")){
            this.offsetMinutes = HubProperties.getProperty("verify.one", 15);
        }else if(host.contains("2.")){
            this.offsetMinutes = HubProperties.getProperty("verify.two", 30);
        }else if(host.contains("2.")){
            this.offsetMinutes = HubProperties.getProperty("verify.three", 45);
        }else{
            this.offsetMinutes = 5;
        }
        executorService = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("ContentServiceImpl-%d").build());
    }

    TimeQuery buildTimeQuery(DateTime startTime, String channelName, String location){
        return TimeQuery.builder()
                .channelName(channelName)
                .startTime(startTime)
                .stable(true)
                .unit(TimeUtil.Unit.MINUTES)
                .location(Location.valueOf(location))  // CACHE/LONG_TERM
                .build();
    }

    SortedSet<ContentKey> itemsInCacheButNotLongTerm(DateTime startTime, String channelName){
        TimeQuery cacheQuery = buildTimeQuery(startTime, channelName, "CACHE");
        TimeQuery ltQuery = buildTimeQuery(startTime, channelName, "LONG_TERM");

        SortedSet<ContentKey> cacheKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        SortedSet<ContentKey> ltKeys = Collections.synchronizedSortedSet(new TreeSet<>());
        try {
            CountDownLatch countDownLatch = new CountDownLatch(2);
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    cacheKeys.addAll(cacheContentDao.queryByTime(cacheQuery.getChannelName(), cacheQuery.getStartTime(), cacheQuery.getUnit(), cacheQuery.getTraces()));
                    countDownLatch.countDown();
                }
            });
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    ltKeys.addAll(longTermContentDao.queryByTime(ltQuery.getChannelName(), ltQuery.getStartTime(), ltQuery.getUnit(), ltQuery.getTraces()));
                    countDownLatch.countDown();
                }
            });
            countDownLatch.await(3, TimeUnit.MINUTES);
            cacheKeys.removeAll(ltKeys);
            return cacheKeys;
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }

    }

    public void run() {
        DateTime startTime = DateTime.now()
                .withMinuteOfHour(offsetMinutes);

        Iterable<ChannelConfiguration> channels = channelService.getChannels();
        for (ChannelConfiguration channel : channels) {
            String channelName = channel.getName();
            SortedSet<ContentKey> keysToAdd = itemsInCacheButNotLongTerm(startTime, channelName);
            for (ContentKey key : keysToAdd) {
                s3WriteQueue.add(new ChannelContentKey(channelName, key));
            }
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
