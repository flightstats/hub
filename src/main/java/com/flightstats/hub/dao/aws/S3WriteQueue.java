package com.flightstats.hub.dao.aws;


import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.exception.FailedReadException;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.util.Sleeper;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.*;

@SuppressWarnings("Convert2Lambda")
@Singleton
public class S3WriteQueue {

    private final static Logger logger = LoggerFactory.getLogger(S3WriteQueue.class);

    private static final int THREADS = HubProperties.getProperty("s3.writeQueueThreads", 20);
    private Retryer<Void> retryer = buildRetryer();
    private BlockingQueue<ChannelContentKey> keys = new LinkedBlockingQueue<>(HubProperties.getProperty("s3.writeQueueSize", 40000));
    private ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
    @Inject
    @Named(ContentDao.CACHE)
    private ContentDao spokeContentDao;
    @Inject
    @Named(ContentDao.SINGLE_LONG_TERM)
    private ContentDao s3SingleContentDao;


    @Inject
    public S3WriteQueue() throws InterruptedException {
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
            retryer.call(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    writeContent(key);
                    return null;
                }
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
                Content content = spokeContentDao.read(key.getChannel(), key.getContentKey());
                if (content.getData() == null) {
                    throw new FailedReadException("unable to read " + key.toString());
                }
                s3SingleContentDao.write(key.getChannel(), content);
            } finally {
                ActiveTraces.end();
            }
        }
    }

    public void add(ChannelContentKey key) {
        boolean value = keys.offer(key);
        if (!value) {
            logger.warn("Add to queue failed - out of queue space. key= {}", key);
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
                .retryIfException(new Predicate<Throwable>() {
                    @Override
                    public boolean apply(@Nullable Throwable throwable) {
                        if (throwable != null) {
                            logger.warn("unable to write to S3 " + throwable.getMessage());
                        }
                        return throwable != null;
                    }
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(1000, 1, TimeUnit.MINUTES))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .build();
    }
}
