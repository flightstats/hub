package com.flightstats.datahub.migration;

import com.flightstats.datahub.dao.ChannelService;
import com.google.common.base.Splitter;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Migration is moving from the beta DataHub w/o Time Index to TheHub
 * in Migration, we will presume we are moving forward in time, starting with (nearly) the oldest Item
 * <p/>
 * Secnario:
 * Producers are inserting Items into a DataHub channel
 * TheHub is setup to Migrate a channel from DataHub
 * Migration starts at nearly the oldest Item, and gradually progresses forward to the current item
 * Migration stays up to date, with some amount of lag
 * <p/>
 * At some point after Migration is current with the Source, the Producers want to switch to inserting into the DeiHub.
 * This should not require any changes to TheHub.
 */
public class Migrator {
    private final static Logger logger = LoggerFactory.getLogger(Migrator.class);

    private final ChannelUtils channelUtils;
    private final String sourceUrls;
    private final ChannelService channelService;
    private final CuratorFramework curator;
    private final ScheduledExecutorService executorService;
    private final List<SourceMigrator> migrators = new ArrayList<>();

    @Inject
    public Migrator(ChannelUtils channelUtils, @Named("migration.source.urls") String sourceUrls,
                    ChannelService channelService, CuratorFramework curator) {
        this.channelUtils = channelUtils;
        this.sourceUrls = sourceUrls;
        this.channelService = channelService;
        this.curator = curator;
        executorService = Executors.newScheduledThreadPool(10);
    }

    public void startThreads() {
        if (sourceUrls.isEmpty()) {
            logger.info("nothing to migrate");
            return;
        }
        Iterable<String> iterable = Splitter.on("|").omitEmptyStrings().trimResults().split(sourceUrls);
        Set<String> targetSet = Sets.newHashSet(iterable);
        for (String target : targetSet) {
            logger.info("starting migration of " + target);
            SourceMigrator migrator = new SourceMigrator(target);
            migrators.add(migrator);
            executorService.scheduleWithFixedDelay(migrator, 0, 1, TimeUnit.MINUTES);
        }
    }

    public List<SourceMigrator> getMigrators() {
        return Collections.unmodifiableList(migrators);
    }

    public class SourceMigrator implements Runnable {
        private final String sourceUrl;
        private final Set<String> migratingChannels = new HashSet<>();

        public Set<String> getSourceChannelUrls() {
            return Collections.unmodifiableSet(migratingChannels);
        }

        private SourceMigrator(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }

        @Override
        public void run() {
            Set<String> rawChannels = channelUtils.getChannels(sourceUrl);
            if (rawChannels.isEmpty()) {
                logger.warn("did not find any channels to migrate at " + sourceUrl);
                return;
            }
            Set<String> filtered = new HashSet<>();
            for (String channel : rawChannels) {
                if (channel.startsWith(sourceUrl)) {
                    filtered.add(channel);
                }
            }
            //todo - gfm - 1/23/14 - does this need to support channel removal?
            filtered.removeAll(migratingChannels);
            for (String channelUrl : filtered) {
                logger.info("found new channel " + channelUrl);
                ChannelMigrator migrator = new ChannelMigrator(channelService, channelUrl, channelUtils, curator);
                executorService.scheduleWithFixedDelay(migrator, 0, 15, TimeUnit.SECONDS);
                migratingChannels.add(channelUrl);
            }
        }
    }

}
