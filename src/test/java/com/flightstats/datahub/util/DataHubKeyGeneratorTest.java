package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Test;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataHubKeyGeneratorTest {

    @Test
    public void testNewKeySimple() throws Exception {
        Date date = new Date(12345678910L);
        DataHubKey expected = new DataHubKey(date, (short) 0);

        TimeProvider timeProvider = mock(TimeProvider.class);

        when(timeProvider.getDate()).thenReturn(date);

        DataHubKeyGenerator testClass = new DataHubKeyGenerator(timeProvider);

        DataHubKey result = testClass.newKey();
        assertEquals(expected, result);
    }

    @Test
    public void testNewKey_sequentialDifferentMilliseconds() throws Exception {
        Date date0 = new Date(12345678910L);
        Date date1 = new Date(12345678911L);
        Date date2 = new Date(12345678912L);
        DataHubKey expected0 = new DataHubKey(date0, (short) 0);
        DataHubKey expected1 = new DataHubKey(date1, (short) 0);
        DataHubKey expected2 = new DataHubKey(date2, (short) 0);

        TimeProvider timeProvider = mock(TimeProvider.class);

        when(timeProvider.getDate()).thenReturn(date0, date1, date2);

        DataHubKeyGenerator testClass = new DataHubKeyGenerator(timeProvider);

        assertEquals(expected0, testClass.newKey());
        assertEquals(expected1, testClass.newKey());
        assertEquals(expected2, testClass.newKey());
    }

    @Test
    public void testNewKey_sequentialInSameMilliseconds() throws Exception {
        Date date = new Date(12345678910L);
        DataHubKey expected0 = new DataHubKey(date, (short) 0);
        DataHubKey expected1 = new DataHubKey(date, (short) 1);
        DataHubKey expected2 = new DataHubKey(date, (short) 2);

        TimeProvider timeProvider = mock(TimeProvider.class);

        when(timeProvider.getDate()).thenReturn(date);

        DataHubKeyGenerator testClass = new DataHubKeyGenerator(timeProvider);

        assertEquals(expected0, testClass.newKey());
        assertEquals(expected1, testClass.newKey());
        assertEquals(expected2, testClass.newKey());
    }

    @Test
    public void testNewKey_clockDriftBackwards() throws Exception {
        Date date0 = new Date(12345678912L);
        Date date1 = new Date(12345678911L); //backwards
        Date date2 = new Date(12345678910L); //backwards
        Date date3 = new Date(12345678912L); //forwards (back at starting point actually)
        Date date4 = new Date(12345678913L); //forwards (just after starting point)

        DataHubKey expected0 = new DataHubKey(date0, (short) 0);
        DataHubKey expected1 = new DataHubKey(date0, (short) 1);
        DataHubKey expected2 = new DataHubKey(date0, (short) 2);
        DataHubKey expected3 = new DataHubKey(date0, (short) 3);
        DataHubKey expected4 = new DataHubKey(date4, (short) 0);

        TimeProvider timeProvider = mock(TimeProvider.class);

        when(timeProvider.getDate()).thenReturn(date0, date1, date2, date3, date4);

        DataHubKeyGenerator testClass = new DataHubKeyGenerator(timeProvider);

        assertEquals(expected0, testClass.newKey());
        assertEquals(expected1, testClass.newKey());
        assertEquals(expected2, testClass.newKey());
        assertEquals(expected3, testClass.newKey());
        assertEquals(expected4, testClass.newKey());
    }

    @Test
    public void testNewKey_multithreaded() throws Exception {
        int threadPoolSize = 100;
        Date date1 = new Date(12345678910L);
        Date date2 = new Date(12345678911L);

        TimeProvider timeProvider = mock(TimeProvider.class);

        when(timeProvider.getDate()).thenReturn(date1, date2);

        final DataHubKeyGenerator testClass = new DataHubKeyGenerator(timeProvider);

        ExecutorService threadPool = Executors.newFixedThreadPool(threadPoolSize);

        final CountDownLatch allThreadsRunning = new CountDownLatch(threadPoolSize);
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch finishedLatch = new CountDownLatch(threadPoolSize);
        final Set<DataHubKey> resultKeys = new CopyOnWriteArraySet<>();

        for (int i = 0; i < threadPoolSize; i++) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    allThreadsRunning.countDown();
                    try {
                        startLatch.await();
                    } catch (InterruptedException e) {
                        System.out.println("INTERRUPTED");
                        fail("Didn't expect to be interrupted");
                    }
                    DataHubKey key = testClass.newKey();
                    resultKeys.add(key);
                    finishedLatch.countDown();
                }
            };
            threadPool.submit(runnable);
        }

        allThreadsRunning.await();
        System.out.println("All threads started...firing them off....");
        startLatch.countDown(); //release the hounds
        finishedLatch.await();
        assertEquals(threadPoolSize, resultKeys.size());

    }
}
