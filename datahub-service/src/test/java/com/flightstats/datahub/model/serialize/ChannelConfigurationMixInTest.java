package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.ChannelConfiguration;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class ChannelConfigurationMixInTest {

	@Test
	public void test() throws Exception {
		ObjectMapper mapper = DataHubObjectMapperFactory.construct();

		String json = "{\"name\": \"The Name\", \"creationDate\": 808, \"ttlMillis\": 42 }";
		ChannelConfiguration result = mapper.readValue(json, ChannelConfiguration.class);
		ChannelConfiguration expected = new ChannelConfiguration("The Name", new Date(808), 42L);
		assertEquals(expected, result);
	}
}
