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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

@Singleton
public class S3WriteQueue {

    private final static Logger logger = LoggerFactory.getLogger(S3WriteQueue.class);

    private final ContentDao spokeWriteContentDao;
    private final ContentDao s3SingleContentDao;
    private final MetricsService metricsService;
    private final int writeQueueThreads;
    private final int writeQueueSize;
    private final Retryer<Void> retryer = buildRetryer();
    private final BlockingQueue<ChannelContentKey> keys;
    private final ExecutorService executorService;

    @Inject
    S3WriteQueue(@Named(ContentDao.WRITE_CACHE) ContentDao spokeWriteContentDao,
                 @Named(ContentDao.SINGLE_LONG_TERM) ContentDao s3SingleContentDao,
                 MetricsService metricsService,
                 HubProperties hubProperties) {
        this.spokeWriteContentDao = spokeWriteContentDao;
        this.s3SingleContentDao = s3SingleContentDao;
        this.metricsService = metricsService;
        this.writeQueueThreads = hubProperties.getS3WriteQueueThreads();
        this.writeQueueSize = hubProperties.getS3WriteQueueSize();
        this.keys = new LinkedBlockingQueue<>(writeQueueSize);

        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("S3WriteQueue-%d").build();
        this.executorService = Executors.newFixedThreadPool(writeQueueThreads, threadFactory);

        run();
    }

    private void run() {
        logger.info("queue size {}", writeQueueSize);
        for (int i = 0; i < writeQueueThreads; i++) {
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

    private void write() {
        try {
            ChannelContentKey key = keys.poll(5, TimeUnit.SECONDS);
            if (key != null) {
                metricsService.gauge("s3.writeQueue.used", keys.size());
                metricsService.count("s3.writeQueue.age.removed", key.getAgeMS(), "key:" + key.toString());
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

    public void add(ChannelContentKey key) {
        boolean value = keys.offer(key);
        if (value) {
            metricsService.gauge("s3.writeQueue.used", keys.size());
            metricsService.count("s3.writeQueue.age.added", key.getAgeMS(), "key:" + key.toString());
        } else {
            logger.warn("Add to queue failed - out of queue space. key= {}", key);
            metricsService.increment("s3.writeQueue.dropped");
        }
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
