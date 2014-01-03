package com.flightstats.datahub.app;

import com.flightstats.datahub.dao.ChannelService;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.google.common.base.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class should achieve a precise level of throughput per second.
 * The goal is to maintain throughput and measure latency and ProvisionedThroughputExceededException
 */
public class ThroughputDriver {

    private final static Logger logger = LoggerFactory.getLogger(ThroughputDriver.class);
    private final static byte[] data = new byte[]{1, 2, 3, 4, 5, 6, 7, 8};

    private final AtomicInteger successCounter = new AtomicInteger(0);
    private int ratePerSecond = 0;
    private final double factor;
    private String name;
    private final ChannelService channelService;

    public ThroughputDriver(int ratePerSecond, double factor, ChannelService channelService) {
        this.ratePerSecond = ratePerSecond;
        this.factor = factor;
        this.name = "throughput" + ratePerSecond;
        this.channelService = channelService;
    }

    public void start() {
        createChannel();
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(ratePerSecond + 1);
        executorService.scheduleAtFixedRate(new CounterRunnable(), 0, 1, TimeUnit.SECONDS);
        Random random = new Random();
        for (int i = 0; i < ratePerSecond; i++) {
            executorService.scheduleAtFixedRate(new ThroughputRunnable(), random.nextInt(1000), 1000, TimeUnit.MILLISECONDS);
        }
    }

    private void createChannel() {
        ChannelConfiguration configuration = ChannelConfiguration.builder()
                .withName(name)
                .withPeakRequestRate(ratePerSecond)
                .withRateTimeUnit(TimeUnit.SECONDS)
                .withContentKiloBytes(1)
                .withType(ChannelConfiguration.ChannelType.TimeSeries)
                .build();
        channelService.createChannel(configuration);
    }

    class ThroughputRunnable implements Runnable {

        @Override
        public void run() {
            try {
                channelService.insert(name, Optional.<String>absent(), Optional.<String>absent(), data);
                successCounter.incrementAndGet();
            } catch (Exception e) {
                logger.info("rate " + ratePerSecond + " exception " + e.getMessage());
            }
        }
    }

    class CounterRunnable implements Runnable {

        int lastCount = 0;

        @Override
        public void run() {
            int current = successCounter.get();
            int diff = current - lastCount;
            lastCount = current;
            if (diff != ratePerSecond) {
                logger.info("rate " + ratePerSecond + " diff " + diff + " current " + current);
            }
        }
    }

}
