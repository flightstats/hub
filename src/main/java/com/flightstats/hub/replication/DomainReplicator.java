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

    private final List<V1ChannelReplicator> v1ChannelReplicators = new ArrayList<>();
    private final ChannelUtils channelUtils;

    private final Provider<V1ChannelReplicator> replicatorProvider;
    private ReplicationDomain domain;
    private ScheduledExecutorService executorService;
    private ScheduledFuture<?> future;

    @Inject
    public DomainReplicator(ChannelUtils channelUtils, Provider<V1ChannelReplicator> replicatorProvider) {
        this.channelUtils = channelUtils;
        this.replicatorProvider = replicatorProvider;
        executorService = Executors.newScheduledThreadPool(1);
    }

    public void start(ReplicationDomain domain) {
        this.domain = domain;
        future = executorService.scheduleWithFixedDelay(this, 0, 1, TimeUnit.MINUTES);
    }

    public Collection<V1ChannelReplicator> getChannels() {
        return Collections.unmodifiableCollection(v1ChannelReplicators);
    }

    public boolean isDifferent(ReplicationDomain newDomain) {
        return !domain.equals(newDomain);
    }

    public void exit() {
        logger.info("exiting " + domain.getDomain());
        future.cancel(true);
        for (V1ChannelReplicator replicator : v1ChannelReplicators) {
            logger.info("exiting " + replicator.getChannel().getName());
            replicator.exit();
        }
        executorService.shutdown();
    }

    @Override
    public void run() {
        Thread.currentThread().setName("DomainReplicator" + domain.getDomain());
        String domainUrl = "http://" + domain.getDomain() + "/channel/";
        Set<Channel> remoteChannels = channelUtils.getChannels(domainUrl);
        if (remoteChannels.isEmpty()) {
            logger.warn("did not find any channels to replicate at " + domainUrl);
            return;
        }
        Set<V1ChannelReplicator> activeReplicators = new HashSet<>();
        Set<String> replicatorNames = new HashSet<>();
        for (V1ChannelReplicator v1ChannelReplicator : v1ChannelReplicators) {
            if (v1ChannelReplicator.isValid()) {
                activeReplicators.add(v1ChannelReplicator);
                replicatorNames.add(v1ChannelReplicator.getChannel().getName());
            }
        }
        v1ChannelReplicators.retainAll(activeReplicators);
        for (Channel channel : remoteChannels) {
            if (domain.getExcludeExcept().contains(channel.getName()) && !replicatorNames.contains(channel.getName())) {
                startChannelReplication(channel, domain);
            }
        }
        Thread.currentThread().setName("EmptyDomainReplicator");
    }

    private void startChannelReplication(Channel channel, ReplicationDomain domain) {
        logger.info("found channel to replicate " + channel);
        V1ChannelReplicator v1ChannelReplicator = replicatorProvider.get();
        v1ChannelReplicator.setChannel(channel);
        v1ChannelReplicator.setHistoricalDays(domain.getHistoricalDays());
        if (v1ChannelReplicator.tryLeadership()) {
            v1ChannelReplicators.add(v1ChannelReplicator);
        }
    }


}
