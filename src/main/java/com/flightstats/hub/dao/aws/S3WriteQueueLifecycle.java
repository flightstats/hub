package com.flightstats.hub.dao.aws;

import com.flightstats.hub.config.S3Property;
import com.flightstats.hub.util.Sleeper;
import com.google.common.util.concurrent.AbstractService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class S3WriteQueueLifecycle extends AbstractService {
    private final S3WriteQueue s3WriteQueue;
    private final S3Property s3Property;
    private ExecutorService executorService;
    private final AtomicBoolean started = new AtomicBoolean(false);

    @Inject
    public S3WriteQueueLifecycle(S3WriteQueue s3WriteQueue, S3Property s3Property) {
        this.s3WriteQueue = s3WriteQueue;
        this.s3Property = s3Property;
        executorService = Executors.newFixedThreadPool(s3Property.getWriteQueueThreadCount(),
                new ThreadFactoryBuilder().setNameFormat("S3WriteQueue-%d").build());
    }

    private void onStart() {
        if (!started.get()) {
            started.set(true);
            notifyStarted();
        }
    }

    public void doStart() {
        log.info("queue capacity {}", s3Property.getWriteQueueSize());
        for (int i = 0; i < s3Property.getWriteQueueThreadCount(); i++) {
            executorService.submit(() -> {
                try {
                    onStart();
                    while (true) {
                        s3WriteQueue.write();
                    }
                } catch (Exception e) {
                    log.warn("exited thread", e);
                    return null;
                }
            });
        }
    }

    public void doStop() {
        int count = 0;
        while (s3WriteQueue.getQueueSize() > 0) {
            count++;
            log.info("waiting for keys {}", s3WriteQueue.getQueueSize());
            if (count >= 60) {
                log.warn("waited too long for keys {}", s3WriteQueue.getQueueSize());
                return;
            }
            Sleeper.sleepQuietly(1000);
        }
        executorService.shutdown();
        notifyStopped();
    }
}
