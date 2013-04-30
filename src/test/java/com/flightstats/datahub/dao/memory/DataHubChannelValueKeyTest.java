package com.flightstats.datahub.dao.memory;

import com.flightstats.datahub.model.DataHubKey;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertEquals;

public class DataHubChannelValueKeyTest {

	@Test
	public void testAsDataHubKey() throws Exception {
		DataHubKey dataHubKey = new DataHubKey(new Date(), (short) 5);
		DataHubChannelValueKey testInstance = new DataHubChannelValueKey(dataHubKey, "channelName");
		assertEquals(dataHubKey, testInstance.asDataHubKey());
	}

}
