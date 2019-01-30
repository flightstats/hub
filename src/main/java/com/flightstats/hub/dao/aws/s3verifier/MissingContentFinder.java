package com.flightstats.hub.dao.aws.s3verifier;

import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.dao.QueryResult;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.RuntimeInterruptedException;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class MissingContentFinder {
    private static final String VERIFIER_TIMEOUT_METRIC_NAME = "s3.verifier.timeout";
    private static final Logger logger = LoggerFactory.getLogger(MissingContentFinder.class);

    private final VerifierConfig verifierConfig;
    private final ExecutorService queryThreadPool;
    private final ContentDao spokeWriteContentDao;
    private final ContentDao s3SingleContentDao;
    private final MetricsService metricsService;

    @Inject
    public MissingContentFinder(@Named(ContentDao.WRITE_CACHE) ContentDao spokeWriteContentDao,
                                @Named(ContentDao.SINGLE_LONG_TERM) ContentDao s3SingleContentDao,
                                VerifierConfig verifierConfig,
                                MetricsService metricsService,
                                @Named("s3VerifierQueryThreadPool") ExecutorService queryThreadPool) {
        this.spokeWriteContentDao = spokeWriteContentDao;
        this.s3SingleContentDao = s3SingleContentDao;
        this.metricsService = metricsService;
        this.verifierConfig = verifierConfig;
        this.queryThreadPool = queryThreadPool;
    }

    public SortedSet<ContentKey> getMissing(MinutePath startPath, MinutePath endPath, String channelName) {
        long timeout = verifierConfig.getBaseTimeoutMinutes();
        QueryResult queryResult = new QueryResult(1);
        SortedSet<ContentKey> s3Keys = new TreeSet<>();
        SortedSet<ContentKey> spokeKeys = new TreeSet<>();
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startPath.getTime())
                .unit(TimeUtil.Unit.MINUTES);
        if (endPath != null) {
            Duration duration = new Duration(startPath.getTime(), endPath.getTime());
            timeout += duration.getStandardDays();
            builder.limitKey(ContentKey.lastKey(endPath.getTime()));
        }
        TimeQuery timeQuery = builder.build();
        try {
            CountDownLatch latch = new CountDownLatch(2);
            runInQueryPool(ActiveTraces.getLocal(), latch, () -> spokeKeys.addAll(spokeWriteContentDao.queryByTime(timeQuery)));
            runInQueryPool(ActiveTraces.getLocal(), latch, () -> s3Keys.addAll(s3SingleContentDao.queryByTime(timeQuery)));
            latch.await(timeout, TimeUnit.MINUTES);
            if (latch.getCount() != 0) {
                logger.error("s3 verifier timed out while finding missing items, write queue is backing up");
                metricsService.increment(VERIFIER_TIMEOUT_METRIC_NAME);
                return new TreeSet<>();
            }
            spokeKeys.removeAll(s3Keys);
            if (spokeKeys.size() > 0) {
                logger.info("missing items {} {}", channelName, queryResult.getContentKeys());
            }
            return spokeKeys;
//            throw new FailedQueryException("unable to query spoke");
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    private void runInQueryPool(Traces traces, CountDownLatch countDownLatch, Runnable runnable) {
        queryThreadPool.submit(() -> {
            ActiveTraces.setLocal(traces);
            try {
                runnable.run();
            } finally {
                countDownLatch.countDown();
            }
        });
    }
}
