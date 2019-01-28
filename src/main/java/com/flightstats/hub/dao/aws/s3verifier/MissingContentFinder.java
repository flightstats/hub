package com.flightstats.hub.dao.aws.s3Verifier;

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
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;

public class MissingContentFinder {
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
        long timeout = calculateQueryTimeout(startPath, endPath);
        TimeQuery timeQuery = buildTimeQuery(channelName, startPath, endPath);
        try {
            CountDownLatch latch = new CountDownLatch(2);
            QueryResult s3QueryResult = new QueryResult(1);
            QueryResult spokeQueryResult = new QueryResult(1);
            runInQueryPool(ActiveTraces.getLocal(), latch, () -> spokeQueryResult.addKeys(spokeWriteContentDao.queryByTime(timeQuery)));
            runInQueryPool(ActiveTraces.getLocal(), latch, () -> s3QueryResult.addKeys(s3SingleContentDao.queryByTime(timeQuery)));
            latch.await(timeout, verifierConfig.getBaseTimeoutUnit());
            if (latch.getCount() != 0) {
                logger.error("s3 verifier timed out while finding missing items, write queue is backing up");
                metricsService.increment(VerifierMetrics.TIMEOUT.getName());
                return new TreeSet<>();
            }
            return findMissingKeys(channelName, s3QueryResult, spokeQueryResult);
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        }
    }

    private SortedSet<ContentKey> findMissingKeys(String channelName, QueryResult spokeQueryResult, QueryResult s3QueryResult) {
        SortedSet<ContentKey> missingKeys = new TreeSet<>(s3QueryResult.getContentKeys());
        missingKeys.removeAll(spokeQueryResult.getContentKeys());
        if (missingKeys.size() > 0) {
            logger.info("missing items {} {}", channelName, missingKeys);
        }
        return missingKeys;
    }

    private TimeQuery buildTimeQuery(String channelName, MinutePath startPath, MinutePath endPath) {
        TimeQuery.TimeQueryBuilder builder = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startPath.getTime())
                .unit(TimeUtil.Unit.MINUTES);

        Optional.ofNullable(endPath)
                .map(MinutePath::getTime)
                .map(ContentKey::lastKey)
                .ifPresent(builder::limitKey);

        return builder.build();
    }

    private long calculateQueryTimeout(MinutePath startPath, MinutePath endPath) {
        long timeout = verifierConfig.getBaseTimeoutValue();
        long timeoutAdjustmentForExcessiveDelays = Optional.ofNullable(endPath)
                .map(MinutePath::getTime)
                .map(endTime -> new Duration(startPath.getTime(), endTime))
                .map(Duration::getStandardDays)
                .orElse(0L) ;
        return timeout + timeoutAdjustmentForExcessiveDelays;
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
