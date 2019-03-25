package com.flightstats.hub.dao.aws.s3Verifier;

import com.flightstats.hub.dao.ContentDao;
import com.flightstats.hub.metrics.StatsdReporter;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.model.TimeQuery;
import com.flightstats.hub.util.TimeUtil;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.junit.Test;

import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import static com.google.common.collect.Lists.newArrayList;
import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MissingContentFinderTest {
    private final StatsdReporter statsdReporter = mock(StatsdReporter.class);
    private final CurrentThreadExecutor queryThreadPool = new CurrentThreadExecutor();
    private final ContentDao spokeWriteContentDao = mock(ContentDao.class);
    private final ContentDao s3SContentDao = mock(ContentDao.class);

    private final DateTime now = DateTime.now();
    private final MinutePath startPath = new MinutePath(now.minusMinutes(20));
    private final MinutePath endPath = new MinutePath(now.minusMinutes(1));
    private final String channelName = "channelName";
    private final VerifierConfig defaultConfig = VerifierConfig.builder()
            .baseTimeoutValue(2)
            .baseTimeoutUnit(TimeUnit.MINUTES)
            .build();

    private final ContentKey contentKey1 = new ContentKey(now.minusMinutes(1));
    private final ContentKey contentKey2 = new ContentKey(now.minusMinutes(2));
    private final ContentKey contentKey3 = new ContentKey(now.minusMinutes(3));

    @Test
    public void testReturnsContentKeysThatAreInSpokeButNotS3() {
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startPath.getTime())
                .unit(TimeUtil.Unit.MINUTES)
                .limitKey(ContentKey.lastKey(endPath.getTime()))
                .build();

        when(spokeWriteContentDao.queryByTime(timeQuery)).thenReturn(buildSet(contentKey1, contentKey2, contentKey3));
        when(s3SContentDao.queryByTime(timeQuery)).thenReturn(buildSet(contentKey2));

        MissingContentFinder missingContentFinder = new MissingContentFinder(spokeWriteContentDao, s3SContentDao, defaultConfig, statsdReporter, queryThreadPool);
        SortedSet<ContentKey> missing = missingContentFinder.getMissing(startPath, endPath, channelName);
        assertEquals(buildSet(contentKey1, contentKey3), missing);
    }

    @Test
    public void testWithoutEndPath_returnsContentKeysThatAreInSpokeButNotS3() {
        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startPath.getTime())
                .unit(TimeUtil.Unit.MINUTES)
                .build();

        when(spokeWriteContentDao.queryByTime(timeQuery)).thenReturn(buildSet(contentKey1, contentKey2, contentKey3));
        when(s3SContentDao.queryByTime(timeQuery)).thenReturn(buildSet(contentKey2));

        MissingContentFinder missingContentFinder = new MissingContentFinder(spokeWriteContentDao, s3SContentDao, defaultConfig, statsdReporter, queryThreadPool);
        SortedSet<ContentKey> missing = missingContentFinder.getMissing(startPath, null, channelName);
        assertEquals(buildSet(contentKey1, contentKey3), missing);
    }

    @Test
    public void testWithExcessivelyLongDurationBetweenStartAndEnd_waitsAnExtraBaseTimeoutUnitPerDayBehind_and_returnsContentKeysThatAreInSpokeButNotS3() {
        int daysBehindInVerification = 3;
        MinutePath ancientStartPath = new MinutePath(now.minusDays(daysBehindInVerification));

        int baseTimeoutInSeconds = 1;
        VerifierConfig configWithShortTimeout = defaultConfig
                .withBaseTimeoutUnit(TimeUnit.SECONDS)
                .withBaseTimeoutValue(baseTimeoutInSeconds);

        Duration executionDelay = Duration.standardSeconds(baseTimeoutInSeconds + 1);
        ExecutorService executorService = Executors.newFixedThreadPool(2, buildSlowThread(executionDelay));

        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channelName)
                .startTime(ancientStartPath.getTime())
                .unit(TimeUtil.Unit.MINUTES)
                .limitKey(ContentKey.lastKey(endPath.getTime()))
                .build();

        when(spokeWriteContentDao.queryByTime(timeQuery)).thenReturn(buildSet(contentKey1, contentKey2, contentKey3));
        when(s3SContentDao.queryByTime(timeQuery)).thenReturn(buildSet(contentKey2));

        MissingContentFinder missingContentFinder = new MissingContentFinder(spokeWriteContentDao, s3SContentDao, configWithShortTimeout, statsdReporter, executorService);

        SortedSet<ContentKey> missing = missingContentFinder.getMissing(ancientStartPath, endPath, channelName);
        assertEquals(buildSet(contentKey1, contentKey3), missing);
    }

    @Test
    public void testWithExecutionExceedingTimeout_returnsAnEmptySet_and_logsTheError() {
        int baseTimeoutInSeconds = 1;
        VerifierConfig configWithShortTimeout = defaultConfig
                .withBaseTimeoutUnit(TimeUnit.SECONDS)
                .withBaseTimeoutValue(baseTimeoutInSeconds);

        Duration executionDelay = Duration.standardSeconds(baseTimeoutInSeconds + 5);
        ExecutorService executorService = Executors.newFixedThreadPool(2, buildSlowThread(executionDelay));

        TimeQuery timeQuery = TimeQuery.builder()
                .channelName(channelName)
                .startTime(startPath.getTime())
                .unit(TimeUtil.Unit.MINUTES)
                .limitKey(ContentKey.lastKey(endPath.getTime()))
                .build();

        when(spokeWriteContentDao.queryByTime(timeQuery)).thenReturn(buildSet(contentKey1, contentKey2, contentKey3));
        when(s3SContentDao.queryByTime(timeQuery)).thenReturn(buildSet(contentKey2));

        MissingContentFinder missingContentFinder = new MissingContentFinder(spokeWriteContentDao, s3SContentDao, configWithShortTimeout, statsdReporter, executorService);

        SortedSet<ContentKey> missing = missingContentFinder.getMissing(startPath, endPath, channelName);
        assertTrue(missing.isEmpty());
        verify(statsdReporter).increment(VerifierMetrics.TIMEOUT.getName());
    }

    private TreeSet<ContentKey> buildSet(ContentKey... contentKeys) {
        return new TreeSet<>(newArrayList(contentKeys));
    }

    private static ThreadFactory buildSlowThread(Duration executionDelay) {
        final AtomicLong count = new AtomicLong(0);

        return runnable -> {
            Runnable slowRunnable = () -> {
                try {
                    Thread.sleep(executionDelay.getMillis());
                    runnable.run();
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            };

            Thread thread = new Thread(slowRunnable);
            thread.setName("test-" + count.getAndIncrement());
            thread.setDaemon(false);
            thread.setPriority(Thread.NORM_PRIORITY);
            return thread;
        };
    }

    private static class CurrentThreadExecutor extends AbstractExecutorService {
        private boolean shutdown = false;
        public void execute(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return emptyList();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }
    }
}
