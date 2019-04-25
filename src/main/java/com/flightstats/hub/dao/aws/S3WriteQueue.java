package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.S3Property;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.exception.FailedReadException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.StatsdReporter;
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
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("WeakerAccess")
@Singleton
@Slf4j
public class S3WriteQueue {

    private Retryer<Void> retryer = buildRetryer();
    private BlockingQueue<ChannelContentKey> keys;
    private ExecutorService executorService;

    private ContentDao spokeWriteContentDao;
    private ContentDao s3SingleContentDao;
    private StatsdReporter statsdReporter;

    @Inject
    public S3WriteQueue(@Named(ContentDao.WRITE_CACHE) ContentDao spokeWriteContentDao,
                        @Named(ContentDao.SINGLE_LONG_TERM) ContentDao s3SingleContentDao,
                        StatsdReporter statsdReporter,
                        S3Property s3Property) {
        this.spokeWriteContentDao = spokeWriteContentDao;
        this.s3SingleContentDao = s3SingleContentDao;
        this.statsdReporter = statsdReporter;

        this.keys = new LinkedBlockingQueue<>(s3Property.getWriteQueueSize());
        this.executorService = Executors.newFixedThreadPool(s3Property.getWriteQueueThreadCount(),
                new ThreadFactoryBuilder().setNameFormat("S3WriteQueue-%d").build());

        log.info("queue size {}", s3Property.getWriteQueueSize());
        write(s3Property.getWriteQueueThreadCount());
    }

    private void write(int queueThreadCount) {

        for (int i = 0; i < queueThreadCount; i++) {
            executorService.submit(() -> {
                try {
                    while (true) {
                        write();
                    }
                } catch (Exception e) {
                    log.warn("exited thread", e);
                    return null;
                }
            });
        }
    }

    private void write() {
        try {
            ChannelContentKey key = keys.poll(5, TimeUnit.SECONDS);
            if (key != null) {
                statsdReporter.gauge("s3.writeQueue.used", keys.size());
                statsdReporter.time("s3.writeQueue.age.removed", key.getAgeMS());
            }
            retryer.call(() -> {
                writeContent(key);
                return null;
            });
        } catch (Exception e) {
            log.warn("unable to call s3", e);
        }
    }

    private void writeContent(ChannelContentKey key) throws Exception {
        if (key != null) {
            ActiveTraces.start("S3WriteQueue.writeContent", key);
            try {
                log.trace("writing {}", key.getContentKey());
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
            statsdReporter.gauge("s3.writeQueue.used", keys.size());
            statsdReporter.time("s3.writeQueue.age.added", key.getAgeMS());
        } else {
            log.warn("Add to queue failed - out of queue space. key= {}", key);
            statsdReporter.increment("s3.writeQueue.dropped");
        }
        return value;
    }

    public void close() {
        int count = 0;
        while (keys.size() > 0) {
            count++;
            log.info("waiting for keys {}", keys.size());
            if (count >= 60) {
                log.warn("waited too long for keys {}", keys.size());
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
                        log.warn("unable to write to S3 " + throwable.getMessage());
                    }
                    return throwable != null;
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(1000, 1, TimeUnit.MINUTES))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
    }
}
