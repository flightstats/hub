package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.ChannelUpdateRequest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChannelUpdateRequestMixInTest {

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = DataHubObjectMapperFactory.construct();
    }

	@Test
	public void testDeserializeWithTtl() throws Exception {
		String json = "{ \"ttlMillis\": \"30000\" }";
		ChannelUpdateRequest result = objectMapper.readValue(json, ChannelUpdateRequest.class);
		ChannelUpdateRequest expected = ChannelUpdateRequest.builder().withTtlMillis(30000L).build();
		assertEquals(expected, result);
	}

	@Test
	public void testDeserializeWithNoTtl() throws Exception {
		String json = "{}";
		ChannelUpdateRequest result = objectMapper.readValue(json, ChannelUpdateRequest.class);
		ChannelUpdateRequest expected = ChannelUpdateRequest.builder().build();
		assertEquals(expected, result);
	}

	@Test
	public void testDeserializeWithNullTtl() throws Exception {
		String json = "{ \"ttlMillis\": null }";
		ChannelUpdateRequest result = objectMapper.readValue(json, ChannelUpdateRequest.class);
		ChannelUpdateRequest expected = ChannelUpdateRequest.builder().withTtlMillis(null).build();
		assertEquals(expected, result);
	}
}
