package com.flightstats.datahub.service;

import com.flightstats.datahub.cluster.ChannelLockFactory;
import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.LinkedDataHubCompositeValue;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static org.mockito.Mockito.*;

public class DataHubSweeperTest {

	@Test
	public void testSweepEmptyChannel() {
		// GIVEN
		ChannelLockFactory channelLockFactory = mock(ChannelLockFactory.class);
		ChannelDao dao = mock(ChannelDao.class);
		ChannelConfiguration channel = new ChannelConfiguration("aChannel", new Date(), 1000L);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel);
		Lock lock = new ReentrantLock();

		// WHEN
		when(dao.getChannels()).thenReturn(channels);
		when(dao.findFirstId(channel.getName())).thenReturn(Optional.<DataHubKey>absent());
		when(channelLockFactory.newLock(channel.getName())).thenReturn(lock);
		DataHubSweeper.SweeperTask testClass = new DataHubSweeper.SweeperTask(dao, channelLockFactory);
		testClass.run();

		// THEN
		verify(dao).getChannels();
		verify(dao).findFirstId(channel.getName());
		verify(dao).delete(channel.getName(), Collections.<DataHubKey>emptyList());
	}

	@Test
	public void testSweepNothingToReap() {
		// GIVEN
		Optional<DataHubKey> hubKey = Optional.of( new DataHubKey(new Date(), (short) 0));
		ChannelLockFactory channelLockFactory = mock(ChannelLockFactory.class);
		ChannelDao dao = mock(ChannelDao.class);
		ChannelConfiguration channel = new ChannelConfiguration("aChannel", new Date(), 100000L);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel);
		Lock lock = new ReentrantLock();

		// WHEN
		when(dao.getChannels()).thenReturn(channels);
		when(dao.findFirstId(channel.getName())).thenReturn(hubKey);
		when(channelLockFactory.newLock(channel.getName())).thenReturn(lock);
		DataHubSweeper.SweeperTask testClass = new DataHubSweeper.SweeperTask(dao, channelLockFactory);
		testClass.run();

		// THEN
		verify(dao).getChannels();
		verify(dao).findFirstId(channel.getName());
		verify(dao).delete(channel.getName(), Collections.<DataHubKey>emptyList());
	}

	@Test
	public void testSweepPartialReap() {
		// GIVEN
		long ttl = 100000L;
		Optional<DataHubKey> reapHubKey = Optional.of( new DataHubKey(new Date( System.currentTimeMillis() - (ttl+1)), (short) 0));
		Optional<DataHubKey> keepHubKey = Optional.of( new DataHubKey(new Date(), (short) 0));
		ChannelLockFactory channelLockFactory = mock(ChannelLockFactory.class);
		ChannelDao dao = mock(ChannelDao.class);
		Optional<LinkedDataHubCompositeValue> reapHubKeyValue = Optional.of(mock(LinkedDataHubCompositeValue.class));
		ChannelConfiguration channel = new ChannelConfiguration("aChannel", new Date(), ttl);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel);
		Lock lock = new ReentrantLock();

		// WHEN
		when(dao.getChannels()).thenReturn(channels);
		when(dao.findFirstId(channel.getName())).thenReturn(reapHubKey);
		when(channelLockFactory.newLock(channel.getName())).thenReturn(lock);
		when(dao.getValue(channel.getName(), reapHubKey.get())).thenReturn(reapHubKeyValue);
		when(reapHubKeyValue.get().hasNext()).thenReturn(true);
		when(reapHubKeyValue.get().getNext()).thenReturn(keepHubKey);
		DataHubSweeper.SweeperTask testClass = new DataHubSweeper.SweeperTask(dao, channelLockFactory);
		testClass.run();

		// THEN
		verify(dao).getChannels();
		verify(dao).findFirstId(channel.getName());
		verify(dao).delete(channel.getName(), Arrays.asList(reapHubKey.get()));
		verify(dao).setFirstKey(channel.getName(), keepHubKey.get());
	}

	@Test
	public void testSweepFullReap() {
		// GIVEN
		long ttl = 100000L;
		Optional<DataHubKey> reapHubKey1 = Optional.of( new DataHubKey(new Date( System.currentTimeMillis() - (ttl+2)), (short) 0));
		Optional<DataHubKey> reapHubKey2 = Optional.of(new DataHubKey(new Date(System.currentTimeMillis() - (ttl+1)), (short) 0));
		ChannelLockFactory channelLockFactory = mock(ChannelLockFactory.class);
		ChannelDao dao = mock(ChannelDao.class);
		Optional<LinkedDataHubCompositeValue> reapHubKey1Value = Optional.of(mock(LinkedDataHubCompositeValue.class));
		Optional<LinkedDataHubCompositeValue> reapHubKey2Value = Optional.of(mock(LinkedDataHubCompositeValue.class));
		ChannelConfiguration channel = new ChannelConfiguration("aChannel", new Date(), ttl);
		Iterable<ChannelConfiguration> channels = Arrays.asList(channel);
		Lock lock = new ReentrantLock();

		// WHEN
		when(dao.getChannels()).thenReturn(channels);
		when(dao.findFirstId(channel.getName())).thenReturn(reapHubKey1);
		when(channelLockFactory.newLock(channel.getName())).thenReturn(lock);
		when(dao.getValue(channel.getName(), reapHubKey1.get())).thenReturn(reapHubKey1Value);
		when(dao.getValue(channel.getName(), reapHubKey2.get())).thenReturn(reapHubKey2Value);
		when(reapHubKey1Value.get().hasNext()).thenReturn(true);
		when(reapHubKey1Value.get().getNext()).thenReturn(reapHubKey2);
		when(reapHubKey2Value.get().hasNext()).thenReturn(false);
		when(reapHubKey2Value.get().getNext()).thenReturn(Optional.<DataHubKey>absent());
		DataHubSweeper.SweeperTask testClass = new DataHubSweeper.SweeperTask(dao, channelLockFactory);
		testClass.run();

		// THEN
		verify(dao).getChannels();
		verify(dao).findFirstId(channel.getName());
		verify(dao).delete(channel.getName(), Arrays.asList(reapHubKey1.get(), reapHubKey2.get()));
		verify(dao).deleteFirstKey(channel.getName());
		verify(dao).deleteLastUpdateKey(channel.getName());
	}
}
