package com.flightstats.hub.spoke;

import com.amazonaws.util.StringUtils;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.ChannelConfig;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.AbstractScheduledService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Singleton
public class SpokeTtlEnforcer {
    private final static Logger logger = LoggerFactory.getLogger(SpokeTtlEnforcer.class);
    private final String storagePath;
    private final int ttlMinutes;
    private final ChannelService channelService;

    @Inject
    public SpokeTtlEnforcer(ChannelService channelService) {
        this.channelService = channelService;
        this.storagePath = HubProperties.getProperty("spoke.path", "/spoke");
        this.ttlMinutes = HubProperties.getProperty("spoke.ttlMinutes", 60);
        if (HubProperties.getProperty("spoke.enforceTTL", true)) {
            HubServices.register(new SpokeTtlEnforcerService());
            HubServices.register(new SpokeTtlEnforcerInitialService());
        }
    }

    public void run() {
        File spokeRoot = new File(storagePath);
        Set<String> dirSet = new HashSet<>(Arrays.asList(spokeRoot.list()));
        DateTime ttlDateTime = TimeUtil.stable().minusMinutes(ttlMinutes + 1);
        String minutes = TimeUtil.minutes(ttlDateTime);
        String hours = TimeUtil.hours(ttlDateTime.minusHours(1));
        Iterable<ChannelConfig> channels = channelService.getChannels();
        Set<String> channelSet = new HashSet<>();
        for (ChannelConfig channel : channels) {
            String channelPath = storagePath + "/" + channel.getName();
            channelSet.add(channel.getName());
            if (channel.isReplicating()) {
                runCommand(new String[]{"find", channelPath, "-mmin", "+" + ttlMinutes, "-delete"});
            } else {
                runCommand(new String[]{"rm", "-rf", channelPath + "/" + minutes});
                runCommand(new String[]{"rm", "-rf", channelPath + "/" + hours});
            }
        }
        dirSet.removeAll(channelSet);
        dirSet.remove("lost+found");
        for (String dir : dirSet) {
            String dirPath = storagePath + "/" + dir;
            logger.info("removing dir without channel {}", dirPath);
            runCommand(new String[]{"rm", "-rf", dirPath});
        }
    }

    private void runCommand(String[] command) {
        try {
            logger.debug("running " + StringUtils.join(" ", command));
            long start = System.currentTimeMillis();
            Process process = new ProcessBuilder(command)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .start();
            boolean waited = process.waitFor(1, TimeUnit.MINUTES);
            long time = System.currentTimeMillis() - start;
            if (waited) {
                logger.debug("waited " + waited + " for " + time);
            } else {
                logger.debug("destroying after " + time);
                process.destroyForcibly();
            }

        } catch (Exception e) {
            logger.warn("unable to enforce ttl", e);
        }
    }

    private class SpokeTtlEnforcerService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() throws Exception {
            run();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedDelaySchedule(1, 1, TimeUnit.MINUTES);
        }

    }

    private class SpokeTtlEnforcerInitialService extends AbstractIdleService {
        @Override
        protected void startUp() throws Exception {
            logger.info("performing Spoke cleanup");
            runCommand(new String[]{"find", storagePath, "-mmin", "+" + ttlMinutes, "-delete"});
            logger.info("completed Spoke cleanup");
        }

        @Override
        protected void shutDown() throws Exception {

        }
    }
}
