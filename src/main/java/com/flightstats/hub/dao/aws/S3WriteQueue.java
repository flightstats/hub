package com.flightstats.hub.dao.aws;


import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.exception.FailedReadException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.util.Sleeper;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

@SuppressWarnings("WeakerAccess")
@Singleton
public class S3WriteQueue {

    private final static Logger logger = LoggerFactory.getLogger(S3WriteQueue.class);

    private static final int THREADS = HubProperties.getS3WriteQueueThreads();
    private static final int QUEUE_SIZE = HubProperties.getS3WriteQueueSize();
    private Retryer<Void> retryer = buildRetryer();
    private BlockingQueue<ChannelContentKey> keys = new LinkedBlockingQueue<>(QUEUE_SIZE);
    private ExecutorService executorService = Executors.newFixedThreadPool(THREADS,
            new ThreadFactoryBuilder().setNameFormat("S3WriteQueue-%d").build());
    @Inject
    @Named(ContentDao.WRITE_CACHE)
    private ContentDao spokeWriteContentDao;
    @Inject
    @Named(ContentDao.SINGLE_LONG_TERM)
    private ContentDao s3SingleContentDao;
    @Inject
    private MetricsService metricsService;

    @Inject
    private S3WriteQueue() throws InterruptedException {
        logger.info("queue size {}", QUEUE_SIZE);
        for (int i = 0; i < THREADS; i++) {
            executorService.submit(() -> {
                try {
                    while (true) {
                        write();
                    }
                } catch (Exception e) {
                    logger.warn("exited thread", e);
                    return null;
                }
            });
        }
    }

    private void write() throws InterruptedException {
        try {
            ChannelContentKey key = keys.poll(5, TimeUnit.SECONDS);
            if (key != null) {
                metricsService.gauge("s3.writeQueue.used", keys.size());
                metricsService.count("s3.writeQueue.age.removed", key.getAgeMS());
            }
            retryer.call(() -> {
                writeContent(key);
                return null;
            });
        } catch (Exception e) {
            logger.warn("unable to call s3", e);
        }
    }

    private void writeContent(ChannelContentKey key) throws Exception {
        if (key != null) {
            ActiveTraces.start("S3WriteQueue.writeContent", key);
            try {
                logger.trace("writing {}", key.getContentKey());
                Content content = spokeWriteContentDao.get(key.getChannel(), key.getContentKey());
                content.packageStream();
                if (content.getData() == null) {
                    throw new FailedReadException("unable to read " + key.toString());
                }
                s3SingleContentDao.insert(key.getChannel(), content);
            } finally {
                ActiveTraces.end();
            }
        }
    }

    public boolean add(ChannelContentKey key) {
        boolean value = keys.offer(key);
        if (value) {
            metricsService.gauge("s3.writeQueue.used", keys.size());
            metricsService.count("s3.writeQueue.age.added", key.getAgeMS());
        } else {
            logger.warn("Add to queue failed - out of queue space. key= {}", key);
            metricsService.increment("s3.writeQueue.dropped");
        }
        return value;
    }

    public void close() {
        int count = 0;
        while (keys.size() > 0) {
            count++;
            logger.info("waiting for keys {}", keys.size());
            if (count >= 60) {
                logger.warn("waited too long for keys {}", keys.size());
                return;
            }
            Sleeper.sleepQuietly(1000);
        }
        executorService.shutdown();
    }

    private Retryer<Void> buildRetryer() {
        return RetryerBuilder.<Void>newBuilder()
                .retryIfException(throwable -> {
                    if (throwable != null) {
                        logger.warn("unable to write to S3 " + throwable.getMessage());
                    }
                    return throwable != null;
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(1000, 1, TimeUnit.MINUTES))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
    }
}
