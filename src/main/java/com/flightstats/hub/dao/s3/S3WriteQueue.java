package com.flightstats.hub.dao.s3;


import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.model.ChannelContentKey;
import com.flightstats.hub.model.Content;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

@SuppressWarnings("Convert2Lambda")
@Singleton
public class S3WriteQueue {

    private final static Logger logger = LoggerFactory.getLogger(S3WriteQueue.class);

    private ExecutorService executorService;
    private BlockingQueue<ChannelContentKey> keys = new LinkedBlockingQueue<>(2000);

    @Inject
    public S3WriteQueue(@Named(ContentDao.CACHE) ContentDao cacheContentDao,
                        @Named(ContentDao.LONG_TERM) ContentDao longTermContentDao,
                        @Named("s3.writeQueueSize") int queueSize,
                        @Named("s3.writeQueueThreads") int threads) throws InterruptedException {
        keys = new LinkedBlockingQueue<>(queueSize);
        executorService = Executors.newFixedThreadPool(threads);
        for (int i = 0; i < threads; i++) {
            executorService.submit(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    //noinspection InfiniteLoopStatement
                    while (true) {
                        ChannelContentKey key = keys.take();
                        if (key != null) {
                            logger.trace("writing {}", key.getContentKey());
                            Content content = cacheContentDao.read(key.getChannel(), key.getContentKey());
                            longTermContentDao.write(key.getChannel(), content);
                            //todo - gfm - 11/21/14 - should this do something else to verify?
                        }
                    }
                }
            });
        }
    }

    public void add(ChannelContentKey key) {
        boolean value = keys.offer(key);
        if(!value){
            logger.debug("Add to queue failed - out of queue space.");
        }
    }

    public void close() {
        try {
            logger.info("awaited " + executorService.awaitTermination(1, TimeUnit.MINUTES));
        } catch (InterruptedException e) {
            logger.warn("unable to close", e);
        }
    }
}
