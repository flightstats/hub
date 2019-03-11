package com.flightstats.hub.dao.aws.s3Verifier;

import com.flightstats.hub.app.NamedDependencies;
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
import java.util.Arrays;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Stream;

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
                                @Named(NamedDependencies.S3_VERIFIER_QUERY_THREAD_POOL) ExecutorService queryThreadPool) {
        this.spokeWriteContentDao = spokeWriteContentDao;
        this.s3SingleContentDao = s3SingleContentDao;
        this.metricsService = metricsService;
        this.verifierConfig = verifierConfig;
        this.queryThreadPool = queryThreadPool;
    }

    public SortedSet<ContentKey> getMissing(MinutePath startPath, MinutePath endPath, String channelName) {
        TimeQuery timeQuery = buildTimeQuery(channelName, startPath, endPath);
        CompletableFuture<QueryResult> spokeQueryResults = getContentKeyFuture(() -> spokeWriteContentDao.queryByTime(timeQuery));
        CompletableFuture<QueryResult> s3QueryResults = getContentKeyFuture(() -> s3SingleContentDao.queryByTime(timeQuery));

        try {
            CompletableFuture
                    .allOf(spokeQueryResults, s3QueryResults)
                    .get(calculateQueryTimeout(startPath, endPath), verifierConfig.getBaseTimeoutUnit());

            return findMissingKeys(channelName, spokeQueryResults.get(), s3QueryResults.get());
        } catch (InterruptedException e) {
            throw new RuntimeInterruptedException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            logger.error("s3 verifier timed out while finding missing items, write queue is backing up");
            metricsService.increment(VerifierMetrics.TIMEOUT.getName());
            return new TreeSet<>();
        } finally {
            Stream.of(spokeQueryResults, s3QueryResults)
                    .filter(future -> !future.isDone())
                    .forEach(future -> future.cancel(true));
        }
    }

    private SortedSet<ContentKey> findMissingKeys(String channelName, QueryResult spokeQueryResult, QueryResult s3QueryResult) {
        SortedSet<ContentKey> missingKeys = new TreeSet<>(spokeQueryResult.getContentKeys());
        missingKeys.removeAll(s3QueryResult.getContentKeys());
        if (missingKeys.size() > 0) {
            logger.info("{} missing items found for {}!", missingKeys.size(), channelName);
            missingKeys.forEach(key -> logger.info("{} missing item {}"));
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

    private CompletableFuture<QueryResult> getContentKeyFuture(Supplier<SortedSet<ContentKey>> callable) {
        Traces traces = ActiveTraces.getLocal();
        return CompletableFuture.supplyAsync(() -> {
            ActiveTraces.setLocal(traces);
            QueryResult queryResult = new QueryResult(1);
            queryResult.addKeys(callable.get());
            return queryResult;
        }, queryThreadPool);
    }
}
