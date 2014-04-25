package com.flightstats.hub.replication;

import com.google.inject.Inject;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
*
*/
public class DomainReplicator implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(DomainReplicator.class);

    private final List<ChannelReplicator> channelReplicators = new ArrayList<>();
    private final ChannelUtils channelUtils;

    private final Provider<ChannelReplicator> replicatorProvider;
    private ReplicationDomain domain;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> future;


    @Inject
    public DomainReplicator(ChannelUtils channelUtils, Provider<ChannelReplicator> replicatorProvider) {
        this.channelUtils = channelUtils;
        this.replicatorProvider = replicatorProvider;
        executorService = Executors.newScheduledThreadPool(1);

    }

    public void start(ReplicationDomain config) {
        this.domain = config;
        future = executorService.scheduleWithFixedDelay(this, 0, 1, TimeUnit.MINUTES);
    }

    public Collection<ChannelReplicator> getChannels() {
        return Collections.unmodifiableCollection(channelReplicators);
    }

    public boolean isDifferent(ReplicationDomain newDomain) {
        return !domain.equals(newDomain);
    }

    public void exit() {
        logger.info("exiting " + domain.getDomain());
        future.cancel(true);
        for (ChannelReplicator replicator : channelReplicators) {
            logger.info("exiting " + replicator.getChannel().getName());
            replicator.exit();
        }
        executorService.shutdown();
    }

    private Set<String> getActiveReplicators() {
        Set<String> actives = new HashSet<>();
        for (ChannelReplicator channelReplicator : channelReplicators) {
            actives.add(channelReplicator.getChannel().getName());
        }
        return actives;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("DomainReplicator" + domain.getDomain());
        String domainUrl = "http://" + domain.getDomain() + "/channel/";
        Set<Channel> channels = channelUtils.getChannels(domainUrl);
        if (channels.isEmpty()) {
            logger.warn("did not find any channels to replicate at " + domainUrl);
            return;
        }
        Set<String> activeReplicators = getActiveReplicators();
        for (Channel channel : channels) {
            if (domain.getExcludeExcept().contains(channel.getName()) && !activeReplicators.contains(channel.getName())) {
                startChannelReplication(channel, domain);
            }
        }
        Thread.currentThread().setName("EmptyDomainReplicator");
    }

    private void startChannelReplication(Channel channel, ReplicationDomain domain) {
        logger.info("found channel to replicate " + channel);
        ChannelReplicator channelReplicator = replicatorProvider.get();
        channelReplicator.setChannel(channel);
        channelReplicator.setHistoricalDays(domain.getHistoricalDays());
        if (channelReplicator.tryLeadership()) {
            channelReplicators.add(channelReplicator);
        }
    }


}
