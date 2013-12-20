package com.flightstats.datahub.cluster;

import com.codahale.metrics.MetricRegistry;
import com.flightstats.datahub.metrics.MetricsTimer;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.SequenceDataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IAtomicLong;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.AlreadyBoundException;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class HazelcastClusterKeyGeneratorTest {

    private MetricsTimer metricsTimer;

    @Before
    public void setUp() throws Exception {
        metricsTimer = new MetricsTimer(new MetricRegistry());

    }

    @Test
    public void testIncrementNoRollover() throws Exception {
        //GIVEN
        String channelName = "mychanisgood";

        HazelcastInstance hazelcast = mock(HazelcastInstance.class);
        ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());
        IAtomicLong atomicSeqNumber = mock(IAtomicLong.class);

        HazelcastClusterKeyGenerator testClass = new HazelcastClusterKeyGenerator(hazelcast, channelLockExecutor, metricsTimer);

        //WHEN
        when(hazelcast.getAtomicLong("CHANNEL_NAME_SEQ:mychanisgood")).thenReturn(atomicSeqNumber);
        when(atomicSeqNumber.getAndAdd(1)).thenReturn(1000L, 1001L);

        //THEN
        assertEquals(new SequenceDataHubKey(1000), testClass.newKey(channelName));
        verify(atomicSeqNumber, times(1)).getAndAdd(1);
        assertEquals(new SequenceDataHubKey(1001), testClass.newKey(channelName));
        verify(atomicSeqNumber, times(2)).getAndAdd(1);
    }

    @Test
    public void testMissingKey() throws Exception {
        String channelName = "missingKey";

        HazelcastInstance hazelcast = mock(HazelcastInstance.class);
        IAtomicLong atomicSeqNumber = mock(IAtomicLong.class);
        ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());

        HazelcastClusterKeyGenerator testClass = new HazelcastClusterKeyGenerator(hazelcast, channelLockExecutor, metricsTimer);

        when(hazelcast.getAtomicLong("CHANNEL_NAME_SEQ:" + channelName)).thenReturn(atomicSeqNumber);
        when(atomicSeqNumber.getAndAdd(1)).thenReturn(0L).thenReturn(1000L);
        when(atomicSeqNumber.get()).thenReturn(0L);

        assertEquals(new SequenceDataHubKey(1000), testClass.newKey(channelName));
        verify(atomicSeqNumber).set(1001L);
        verify(atomicSeqNumber).getAndAdd(1);
    }

    @Test
    public void testKeyAfterLock() throws Exception {
        String channelName = "secondLock";
        DataHubKey latestKey = new SequenceDataHubKey(9999);
        DataHubKey expectedKey = latestKey.getNext().get();

        HazelcastInstance hazelcast = mock(HazelcastInstance.class);
        IAtomicLong atomicSeqNumber = mock(IAtomicLong.class);
        ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());

        HazelcastClusterKeyGenerator testClass = new HazelcastClusterKeyGenerator(hazelcast, channelLockExecutor, metricsTimer);

        when(hazelcast.getAtomicLong("CHANNEL_NAME_SEQ:" + channelName)).thenReturn(atomicSeqNumber);
        when(atomicSeqNumber.getAndAdd(1)).thenReturn(1L).thenReturn(expectedKey.getSequence());
        when(atomicSeqNumber.get()).thenReturn(expectedKey.getSequence());

        assertEquals(expectedKey.getSequence(), testClass.newKey(channelName).getSequence());
        verify(atomicSeqNumber, times(0)).set(anyLong());
        verify(atomicSeqNumber, times(2)).getAndAdd(1);
    }

    @Test(expected = RuntimeException.class)
    public void testExceptionCoerced() throws Exception {
        //GIVEN
        ChannelLockExecutor channelLockExecutor = mock(ChannelLockExecutor.class);

        HazelcastClusterKeyGenerator testClass = new HazelcastClusterKeyGenerator(null, null, metricsTimer);

        //WHEN
        when(channelLockExecutor.execute(anyString(), any(Callable.class))).thenThrow(new AlreadyBoundException());
        testClass.newKey("mychanisgood");
    }
}
