package com.flightstats.datahub.service;

import com.flightstats.datahub.dao.ChannelDao;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.model.ValueInsertionResult;
import com.google.common.base.Optional;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class WriteAndDispatchTest {
	@Test
	public void testCall() throws Exception {
		String channelName = "channelName";
		byte[] data = {'h', 'e', 'l', 'l', 'o'};
		Optional<String> contentType = Optional.of("contentType");
		Optional<String> contentEncoding = Optional.of("contentEncoding");
		Optional<String> contentLanguage = Optional.of("contentLanguage");
		DataHubKey dataHubKey = new DataHubKey((short) 3000);


		ChannelDao channelDao = mock(ChannelDao.class);
		ChannelInsertionPublisher channelInsertionPublisher = mock(ChannelInsertionPublisher.class);

		when(channelDao.insert(channelName, contentType, contentLanguage, data)).thenReturn(new ValueInsertionResult(dataHubKey));

		WriteAndDispatch testClass = new WriteAndDispatch(channelDao, channelInsertionPublisher, channelName, data, contentType, contentLanguage);
		ValueInsertionResult result = testClass.call();

		assertEquals(dataHubKey, result.getKey());
		verify(channelInsertionPublisher).publish(channelName, new ValueInsertionResult(dataHubKey));
	}
}
