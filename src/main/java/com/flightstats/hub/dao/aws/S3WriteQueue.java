package com.flightstats.hub.dao.aws;


import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("Convert2Lambda")
@Singleton
public class S3WriteQueue {

    private final static Logger logger = LoggerFactory.getLogger(S3WriteQueue.class);
    private final Retryer<Void> retryer;

    private ExecutorService executorService;
    private BlockingQueue<ChannelContentKey> keys;
    private ContentDao cacheContentDao;
    private ContentDao longTermContentDao;
    private AtomicBoolean shutdown = new AtomicBoolean(false);

    @Inject
    public S3WriteQueue(@Named(ContentDao.CACHE) ContentDao cacheContentDao,
                        @Named(ContentDao.LONG_TERM) ContentDao longTermContentDao)
            throws InterruptedException {
        this.cacheContentDao = cacheContentDao;
        this.longTermContentDao = longTermContentDao;

        keys = new LinkedBlockingQueue<>(HubProperties.getProperty("s3.writeQueueSize", 40000));
        int threads = HubProperties.getProperty("s3.writeQueueThreads", 20);
        executorService = Executors.newFixedThreadPool(threads);
        retryer = buildRetryer();
        for (int i = 0; i < threads; i++) {
            executorService.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    while (!shutdown.get()) {
                        write();
                    }
                    return null;
                }
            });
        }
    }

    private void write() throws InterruptedException {
        try {
            retryer.call(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    writeContent();
                    return null;
                }
            });
        } catch (Exception e) {
            logger.warn("unable to call s3", e);
        }
    }

    private void writeContent() throws Exception {
        ChannelContentKey key = keys.poll(5, TimeUnit.SECONDS);
        if (key != null) {
            logger.trace("writing {}", key.getContentKey());
            Content content = cacheContentDao.read(key.getChannel(), key.getContentKey());
            longTermContentDao.write(key.getChannel(), content);
        }
    }

    public void add(ChannelContentKey key) {
        boolean value = keys.offer(key);
        if (!value) {
            logger.info("Add to queue failed - out of queue space. key= {}", key);
        }
    }

    public void close() {
        shutdown.set(true);
        executorService.shutdown();
    }

    private Retryer<Void> buildRetryer() {
        return RetryerBuilder.<Void>newBuilder()
                .retryIfException(new Predicate<Throwable>() {
                    @Override
                    public boolean apply(@Nullable Throwable throwable) {
                        if (throwable != null) {
                            logger.warn("unable to send to S3 " + throwable.getMessage());
                        }
                        return throwable != null;
                    }
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(1000, 1, TimeUnit.MINUTES))
                .build();
    }
}
