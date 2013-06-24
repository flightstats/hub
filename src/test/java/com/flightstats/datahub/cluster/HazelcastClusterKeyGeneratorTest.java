package com.flightstats.datahub.cluster;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.service.ChannelLockExecutor;
import com.flightstats.datahub.util.TimeProvider;
import com.hazelcast.core.AtomicNumber;
import com.hazelcast.core.HazelcastInstance;
import org.junit.Test;

import java.nio.channels.AlreadyBoundException;
import java.util.Date;
import java.util.concurrent.Callable;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class HazelcastClusterKeyGeneratorTest {

	@Test
	public void testNonCollision() throws Exception {
		//GIVEN
		String channelName = "mychanisgood";
		Date currentDate = new Date(12345678L);
		DataHubKey expected = new DataHubKey(currentDate, (short) 0);

		TimeProvider timeProvider = mock(TimeProvider.class);
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());
		AtomicNumber atomicDateNumber = mock(AtomicNumber.class);
		AtomicNumber atomicSeqNumber = mock(AtomicNumber.class);

		HazelcastClusterKeyGenerator testClass = new HazelcastClusterKeyGenerator(timeProvider, hazelcast, channelLockExecutor);

		//WHEN
		when(timeProvider.getDate()).thenReturn(currentDate);
		when(atomicDateNumber.get()).thenReturn(currentDate.getTime() - 10);
		when(hazelcast.getAtomicNumber("CHANNEL_NAME_DATE:mychanisgood")).thenReturn(atomicDateNumber);
		when(hazelcast.getAtomicNumber("CHANNEL_NAME_SEQ:mychanisgood")).thenReturn(atomicSeqNumber);
		DataHubKey result = testClass.newKey(channelName);

		//THEN
		assertEquals(expected, result);
		verify(atomicSeqNumber).set(0);
	}

	@Test
	public void testTimeCollision() throws Exception {
		//GIVEN
		String channelName = "mychanisgood";
		Date currentDate = new Date(12345678L);
		DataHubKey expected = new DataHubKey(currentDate, (short) 1);

		TimeProvider timeProvider = mock(TimeProvider.class);
		HazelcastInstance hazelcast = mock(HazelcastInstance.class);
		ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());
		AtomicNumber atomicDateNumber = mock(AtomicNumber.class);
		AtomicNumber atomicSeqNumber = mock(AtomicNumber.class);

		HazelcastClusterKeyGenerator testClass = new HazelcastClusterKeyGenerator(timeProvider, hazelcast, channelLockExecutor);

		//WHEN
		when(timeProvider.getDate()).thenReturn(currentDate);
		when(atomicDateNumber.get()).thenReturn(currentDate.getTime());
		when(hazelcast.getAtomicNumber("CHANNEL_NAME_DATE:mychanisgood")).thenReturn(atomicDateNumber);
		when(hazelcast.getAtomicNumber("CHANNEL_NAME_SEQ:mychanisgood")).thenReturn(atomicSeqNumber);
		when(atomicSeqNumber.addAndGet(1)).thenReturn(1L);
		DataHubKey result = testClass.newKey(channelName);

		//THEN
		assertEquals(expected, result);
	}

	@Test(expected = RuntimeException.class)
	public void testExceptionCoerced() throws Exception {
		//GIVEN
		ChannelLockExecutor channelLockExecutor = mock(ChannelLockExecutor.class);

		HazelcastClusterKeyGenerator testClass = new HazelcastClusterKeyGenerator(null, null, channelLockExecutor);

		//WHEN
		when(channelLockExecutor.execute(anyString(), any(Callable.class))).thenThrow(new AlreadyBoundException());
		testClass.newKey("mychanisgood");
	}
}
