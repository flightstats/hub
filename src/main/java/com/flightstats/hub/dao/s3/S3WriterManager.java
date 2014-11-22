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
        HubServices.register(new S3WriterManagerService(), HubServices.TYPE.POST_START);
    }

    public void run() {
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
            return Scheduler.newFixedDelaySchedule(0, 1, TimeUnit.MINUTES);
        }

        @Override
        protected void shutDown() throws Exception {
            Collection<S3ChannelWriter> writers = channelWriterMap.values();
            for (S3ChannelWriter writer : writers) {
                writer.close();
            }
        }
    }
}
