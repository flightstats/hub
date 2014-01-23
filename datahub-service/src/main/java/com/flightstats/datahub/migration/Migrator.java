package com.flightstats.datahub.migration;

import com.flightstats.datahub.dao.ChannelService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Migration is moving from the beta DataHub w/o Time Index to TheHub
 * in Migration, we will presume we are moving forward in time, starting with (nearly) the oldest Item
 *
 * Secnario:
 * Producers are inserting Items into a DataHub channel
 * TheHub is setup to Migrate a channel from DataHub
 * Migration starts at nearly the oldest Item, and gradually progresses forward to the current item
 * Migration stays up to date, with some amount of lag
 *
 * At some point after Migration is current with the Source, the Producers want to switch to inserting into the DeiHub.
 * This should not require any changes to TheHub.
 *
 */
public class Migrator implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(Migrator.class);

    private final ChannelUtils channelUtils;
    private final String target;
    private final ChannelService channelService;
    private final CuratorFramework curator;
    private final ScheduledExecutorService executorService;
    //todo - gfm - 1/22/14 - expose this through a Resource
    private final Set<String> migratingChannels = new HashSet<>();

    //todo - gfm - 1/22/14 - should migration.target.url accept multiples?
    //todo - gfm - 1/22/14 - would be nice if migration.target.url isn't required/has default
    @Inject
    public Migrator(ChannelUtils channelUtils, @Named("migration.target.url") String target,
                    ChannelService channelService, CuratorFramework curator) {
        this.channelUtils = channelUtils;
        this.target = target;
        this.channelService = channelService;
        this.curator = curator;
        executorService = Executors.newScheduledThreadPool(10);
    }

    @Override
    public void run() {
        Set<String> rawChannels = channelUtils.getChannels(target);
        if (rawChannels.isEmpty()) {
            logger.warn("did not find any channels to migrate at " + target);
            return;
        }
        Set<String> filtered = new HashSet<>();
        for (String channel : rawChannels) {
            if (channel.startsWith(target)) {
                filtered.add(channel);
            }
        }
        filtered.removeAll(migratingChannels);
        logger.info("found new channels " + filtered);
        for (String channelUrl : filtered) {
            ChannelMigrator migrator = new ChannelMigrator(channelService, channelUrl, channelUtils, curator);
            executorService.scheduleWithFixedDelay(migrator, 0, 15, TimeUnit.SECONDS);
            migratingChannels.add(channelUrl);
        }
        //todo - gfm - 1/22/14 - where is the locking mechanism to prevent multiple migrations?
    }

    public void startThread() {
        executorService.scheduleWithFixedDelay(this, 0, 1, TimeUnit.MINUTES);
    }

}
