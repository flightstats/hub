package com.flightstats.hub.dao.s3;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelConfiguration;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class S3WriterManager {

    private final ChannelService channelService;
    private final ContentDao cacheContentDao;
    private final S3WriteQueue s3WriteQueue;
    private final CuratorFramework curator;
    private final Map<String, S3ChannelWriter> channelWriterMap = new HashMap<>();

    @Inject
    public S3WriterManager(ChannelService channelService,
                           @Named(ContentDao.CACHE) ContentDao cacheContentDao,
                           S3WriteQueue s3WriteQueue,
                           CuratorFramework curator) {
        this.channelService = channelService;
        this.cacheContentDao = cacheContentDao;
        this.s3WriteQueue = s3WriteQueue;
        this.curator = curator;
        HubServices.register(new S3WriterManagerService(), HubServices.TYPE.POST_START, HubServices.TYPE.PRE_STOP);
    }

    public void run() {
        // TODO bc 1/5/15: modify this to not use leadership - maybe skip s3channelwriter?
        // 0. find server's offset - from mapping
        // 1. query minute from s3
        // look at ContentServiceImpl.java line 119
        // 2. query minute from cache
        // 3. set difference
        // 4. add difference to s3WriteQueue

        Set<String> allChannels = new HashSet<>();
        Iterable<ChannelConfiguration> channels = channelService.getChannels();
        for (ChannelConfiguration channel : channels) {
            String channelName = channel.getName();
            allChannels.add(channelName);
            if (!channelWriterMap.containsKey(channelName)) {
                S3ChannelWriter writer = new S3ChannelWriter(cacheContentDao, s3WriteQueue, curator);
                channelWriterMap.put(channelName, writer);
                writer.tryLeadership(channelName);
            }
        }
        Set<String> writers = new HashSet<>(channelWriterMap.keySet());
        writers.removeAll(allChannels);
        for (String writer : writers) {
            S3ChannelWriter removed = channelWriterMap.remove(writer);
            removed.close();
        }
    }

    private class S3WriterManagerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            // TODO bc 1/5/15: use customScheduler.getNextSchedule?
            // the intent is to have each server have a particular time it runs during the hour
            // to add items to the s3WriteQueue if they hadn't already been added.
            // find current time
            // get minutes until the time we should run (e.g. 40 minutes after the hour
            // create

            return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.MINUTES);
        }

        @Override
        protected void shutDown() throws Exception {
            Collection<S3ChannelWriter> writers = channelWriterMap.values();
            for (S3ChannelWriter writer : writers) {
                writer.close();
            }
            s3WriteQueue.close();
        }
    }
}
