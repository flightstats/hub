package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.*;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

public class DataHubServiceTest {

	@Test
	public void testGetChannels() throws Exception {
		ChannelConfiguration channel1 = new ChannelConfiguration("channel1", new Date(), 1000L);
		ChannelConfiguration channel2 = new ChannelConfiguration("channel2", new Date(), 2000L);
		List<ChannelConfiguration> channelConfigurations = Arrays.asList(channel1, channel2);

		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.getChannels()).thenReturn(channelConfigurations);

		DataHubService testClass = new DataHubService(channelDao, null);

		Iterable<ChannelConfiguration> channels = testClass.getChannels();
		assertEquals(channelConfigurations, channels);
	}

	@Test
	public void testCreateChannel() throws Exception {
		ChannelDao channelDao = mock(ChannelDao.class);

		DataHubService testClass = new DataHubService(channelDao, null);

		testClass.createChannel("channelName", 1000L);

		verify(channelDao).createChannel("channelName", 1000L);
	}

	@Test
	public void testChannelExists() throws Exception {
		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.channelExists("channelName")).thenReturn(true);

		DataHubService testClass = new DataHubService(channelDao, null);
		boolean result = testClass.channelExists("channelName");
		assertTrue(result);
	}

	@Test
	public void testGetChannelConfiguration() throws Exception {
		ChannelConfiguration channelConfiguration = new ChannelConfiguration("channel1", new Date(), 1000L);

		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.getChannelConfiguration("channelName")).thenReturn(channelConfiguration);

		DataHubService testClass = new DataHubService(channelDao, null);
		ChannelConfiguration result = testClass.getChannelConfiguration("channelName");
		assertEquals(channelConfiguration, result);
	}

	@Test
	public void testFindLastUpdatedKey() throws Exception {
		DataHubKey dataHubKey = new DataHubKey((short) 1033);

		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.findLastUpdatedKey("channelName")).thenReturn(Optional.of(dataHubKey));

		DataHubService testClass = new DataHubService(channelDao, null);

		Optional<DataHubKey> result = testClass.findLastUpdatedKey("channelName");
		assertEquals(dataHubKey, result.get());
	}

    @Test
    public void testInsert() throws Exception {
        String channelName = "channelName";
        byte[] data = {'h', 'e', 'l', 'l', 'o'};
        Optional<String> contentType = Optional.of("contentType");
        Optional<String> contentLanguage = Optional.of("contentLanguage");
        DataHubKey dataHubKey = new DataHubKey(300);
        ChannelDao channelDao = mock(ChannelDao.class);

        ChannelInsertionPublisher channelInsertionPublisher = mock(ChannelInsertionPublisher.class);

        ValueInsertionResult valueInsertionResult = new ValueInsertionResult(dataHubKey, "stuff", new Date());
        when(channelDao.insert(channelName, contentType, contentLanguage, data)).thenReturn(valueInsertionResult);

        DataHubService dataHubService = new DataHubService(channelDao, channelInsertionPublisher);
        ValueInsertionResult result = dataHubService.insert(channelName, data, contentType, contentLanguage);

        assertEquals(dataHubKey, result.getKey());
        verify(channelDao).insert(channelName, contentType, contentLanguage, data);
        verify(channelInsertionPublisher).publish(channelName, valueInsertionResult);
    }

	@Test
	public void testGetValue() throws Exception {
		DataHubKey dataHubKey = new DataHubKey((short) 1033);
		byte[] data = {'h', 'e', 'l', 'l', 'o'};
		Optional<String> contentType = Optional.of("contentType");
		Optional<String> contentEncoding = Optional.of("contentEncoding");
		Optional<String> contentLanguage = Optional.of("contentLanguage");
		LinkedDataHubCompositeValue compositeValue = new LinkedDataHubCompositeValue(new DataHubCompositeValue(contentType, contentLanguage, data, 0L),
				Optional.<DataHubKey>absent(), Optional.<DataHubKey>absent());

		ChannelDao channelDao = mock(ChannelDao.class);
		when(channelDao.getValue("channelName", dataHubKey)).thenReturn(Optional.of(compositeValue));
		DataHubService testClass = new DataHubService(channelDao, null);

		Optional<LinkedDataHubCompositeValue> result = testClass.getValue("channelName", dataHubKey);
		assertEquals(compositeValue, result.get());
	}

	@Test
	public void testUpdateChannelMetadata() throws Exception {
		ChannelConfiguration channelConfiguration = new ChannelConfiguration("channel1", new Date(), 1000L);

		ChannelDao channelDao = mock(ChannelDao.class);
		DataHubService testClass = new DataHubService(channelDao, null);

		testClass.updateChannelMetadata(channelConfiguration);
		verify(channelDao).updateChannelMetadata(channelConfiguration);
	}
}
