package com.flightstats.datahub.replication;

import com.flightstats.datahub.service.eventing.ChannelNameExtractor;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

    /*public Set<String> getSourceChannelUrls() {
        return Collections.unmodifiableSet(replicatingChannels);
    }*/

    public boolean isDifferent(ReplicationDomain newDomain) {
        return !domain.equals(newDomain);
    }

    public void exit() {
        logger.info("exiting " + domain.getDomain());
        future.cancel(true);
        for (ChannelReplicator replicator : channelReplicators) {
            logger.info("exiting " + replicator.getChannelName());
            replicator.exit();
        }
        executorService.shutdown();
    }

    private Set<String> getActiveReplicators() {
        Set<String> actives = new HashSet<>();
        for (ChannelReplicator channelReplicator : channelReplicators) {
            actives.add(channelReplicator.getChannelName());
        }
        return actives;
    }

    @Override
    public void run() {
        String domainUrl = "http://" + domain.getDomain() + "/channel/";
        Set<String> rawChannels = channelUtils.getChannels(domainUrl);
        if (rawChannels.isEmpty()) {
            logger.warn("did not find any channels to replicate at " + domainUrl);
            return;
        }
        //todo - gfm - 1/29/14 - test this
        Set<String> activeReplicators = getActiveReplicators();
        if (domain.isInclusive()) {
            for (String rawChannel : rawChannels) {
                String channel = ChannelNameExtractor.extractFromChannelUrl(rawChannel);
                if (!domain.getIncludeExcept().contains(channel) && !activeReplicators.contains(channel)) {
                    startChannelReplication(rawChannel);
                }
            }
        } else {
            for (String rawChannel : rawChannels) {
                String channel = ChannelNameExtractor.extractFromChannelUrl(rawChannel);
                if (domain.getExcludeExcept().contains(channel) && !activeReplicators.contains(channel)) {
                    startChannelReplication(rawChannel);
                }
            }
        }
    }

    private void startChannelReplication(String channelUrl) {
        logger.info("found new channel to replicate " + channelUrl);
        ChannelReplicator channelReplicator = replicatorProvider.get();
        channelReplicator.setChannelUrl(channelUrl);
        executorService.scheduleWithFixedDelay(channelReplicator, 0, 1, TimeUnit.MINUTES);
        channelReplicators.add(channelReplicator);
    }


}
