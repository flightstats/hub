package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ReentrantChannelLockFactory;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.mockito.Mockito.*;

public class DataHubSweeperTest {

	@Test
<<<<<<< HEAD
=======
	public void testSweepEmptyChannel() {
		// GIVEN
		ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());
		ChannelDao dao = mock(ChannelDao.class);
		ChannelConfiguration channel = new ChannelConfiguration("aChannel", new Date(), 1000L);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel);

		// WHEN
		when(dao.getChannels()).thenReturn(channels);
		when(dao.findFirstUpdateKey(channel.getName())).thenReturn(Optional.<DataHubKey>absent());
		DataHubSweeper.SweeperTask testClass = new DataHubSweeper.SweeperTask(dao, channelLockExecutor);
		testClass.run();

		// THEN
		verify(dao).getChannels();
		verify(dao).findFirstUpdateKey(channel.getName());
		verify(dao).delete(channel.getName(), Collections.<DataHubKey>emptyList());
	}

	@Test
>>>>>>> jason/perfy_perf_perf_perf_perf
	public void testSweepNothingToReap() {
		// GIVEN
		ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());
		ChannelDao dao = mock(ChannelDao.class);
		ChannelConfiguration channel = new ChannelConfiguration("aChannel", new Date(), 100000L);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel);

		// WHEN
		when(dao.getChannels()).thenReturn(channels);
<<<<<<< HEAD
		when(dao.findKeysInRange(anyString(), any(Date.class), any(Date.class))).thenReturn(Collections.<DataHubKey>emptyList());
=======
		when(dao.findFirstUpdateKey(channel.getName())).thenReturn(hubKey);
>>>>>>> jason/perfy_perf_perf_perf_perf
		DataHubSweeper.SweeperTask testClass = new DataHubSweeper.SweeperTask(dao, channelLockExecutor);
		testClass.run();

		// THEN
		verify(dao).getChannels();
<<<<<<< HEAD
		verify(dao).findKeysInRange(anyString(), any(Date.class), any(Date.class));
		verify(dao,times(0)).delete(channel.getName(), Collections.<DataHubKey>emptyList());
=======
		verify(dao).findFirstUpdateKey(channel.getName());
		verify(dao).delete(channel.getName(), Collections.<DataHubKey>emptyList());
>>>>>>> jason/perfy_perf_perf_perf_perf
	}

	@Test
	public void testSweepPartialReap() {
		// GIVEN
		long ttl = 100000L;
		Optional<DataHubKey> reapHubKey = Optional.of(new DataHubKey(new Date(System.currentTimeMillis() - (ttl + 1)), (short) 0));
		Optional<DataHubKey> keepHubKey = Optional.of(new DataHubKey(new Date(), (short) 0));
		ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());
		ChannelDao dao = mock(ChannelDao.class);
		Optional<LinkedDataHubCompositeValue> reapHubKeyValue = Optional.of(mock(LinkedDataHubCompositeValue.class));
		ChannelConfiguration channel = new ChannelConfiguration("aChannel", new Date(), ttl);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel);

		// WHEN
		when(dao.getChannels()).thenReturn(channels);
<<<<<<< HEAD
		when(dao.findKeysInRange(anyString(), any(Date.class), any(Date.class))).thenReturn(Arrays.asList(reapHubKey.get()));
=======
		when(dao.findFirstUpdateKey(channel.getName())).thenReturn(reapHubKey);
>>>>>>> jason/perfy_perf_perf_perf_perf
		when(dao.getValue(channel.getName(), reapHubKey.get())).thenReturn(reapHubKeyValue);
		when(reapHubKeyValue.get().hasNext()).thenReturn(true);
		when(reapHubKeyValue.get().getNext()).thenReturn(keepHubKey);
		DataHubSweeper.SweeperTask testClass = new DataHubSweeper.SweeperTask(dao, channelLockExecutor);
		testClass.run();

		// THEN
		verify(dao).getChannels();
<<<<<<< HEAD
		verify(dao).findKeysInRange(anyString(), any(Date.class), any(Date.class));
=======
		verify(dao).findFirstUpdateKey(channel.getName());
>>>>>>> jason/perfy_perf_perf_perf_perf
		verify(dao).delete(channel.getName(), Arrays.asList(reapHubKey.get()));
		verify(dao).setFirstKey(channel.getName(), keepHubKey.get());
	}

	@Test
	public void testSweepFullReap() {
		// GIVEN
		long ttl = 100000L;
		Optional<DataHubKey> reapHubKey1 = Optional.of(new DataHubKey(new Date(System.currentTimeMillis() - (ttl + 2)), (short) 0));
		Optional<DataHubKey> reapHubKey2 = Optional.of(new DataHubKey(new Date(System.currentTimeMillis() - (ttl + 1)), (short) 0));
		ChannelLockExecutor channelLockExecutor = new ChannelLockExecutor(new ReentrantChannelLockFactory());
		ChannelDao dao = mock(ChannelDao.class);
		Optional<LinkedDataHubCompositeValue> reapHubKey1Value = Optional.of(mock(LinkedDataHubCompositeValue.class));
		Optional<LinkedDataHubCompositeValue> reapHubKey2Value = Optional.of(mock(LinkedDataHubCompositeValue.class));
		ChannelConfiguration channel = new ChannelConfiguration("aChannel", new Date(), ttl);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel);

		// WHEN
		when(dao.getChannels()).thenReturn(channels);
<<<<<<< HEAD
		when(dao.findKeysInRange(anyString(), any(Date.class), any(Date.class))).thenReturn(Arrays.<DataHubKey>asList(reapHubKey1.get(), reapHubKey2.get()));
=======
		when(dao.findFirstUpdateKey(channel.getName())).thenReturn(reapHubKey1);
>>>>>>> jason/perfy_perf_perf_perf_perf
		when(dao.getValue(channel.getName(), reapHubKey1.get())).thenReturn(reapHubKey1Value);
		when(dao.getValue(channel.getName(), reapHubKey2.get())).thenReturn(reapHubKey2Value);
		when(reapHubKey2Value.get().hasNext()).thenReturn(false);
		when(reapHubKey2Value.get().getNext()).thenReturn(Optional.<DataHubKey>absent());
		DataHubSweeper.SweeperTask testClass = new DataHubSweeper.SweeperTask(dao, channelLockExecutor);
		testClass.run();

		// THEN
		verify(dao).getChannels();
<<<<<<< HEAD
		verify(dao).findKeysInRange(anyString(), any(Date.class), any(Date.class));
=======
		verify(dao).findFirstUpdateKey(channel.getName());
>>>>>>> jason/perfy_perf_perf_perf_perf
		verify(dao).delete(channel.getName(), Arrays.asList(reapHubKey1.get(), reapHubKey2.get()));
		verify(dao).deleteFirstKey(channel.getName());
		verify(dao).deleteLastUpdateKey(channel.getName());
	}
}
