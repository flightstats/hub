package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.ChannelCreationRequest;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChannelCreationRequestMixInTest {

    private ObjectMapper objectMapper;

    @Before
    public void setup() {
        objectMapper = new DataHubObjectMapperFactory().build();
    }

	@Test
	public void testDeserializeWithTtl() throws Exception {
		String json = "{ \"name\": \"foo\", \"ttlMillis\": \"30000\" }";
		ChannelCreationRequest result = objectMapper.readValue(json, ChannelCreationRequest.class);
		ChannelCreationRequest expected = ChannelCreationRequest.builder().withName("foo").withTtlMillis(30000L).build();
		assertEquals(expected, result);
	}

	@Test
	public void testDeserializeWithNoTtl() throws Exception {
		String json = "{ \"name\": \"foo\" }";
		ChannelCreationRequest result = objectMapper.readValue(json, ChannelCreationRequest.class);
		ChannelCreationRequest expected = ChannelCreationRequest.builder().withName("foo").withTtlMillis(ChannelCreationRequest.DEFAULT_TTL).build();
		assertEquals(expected, result);
	}

	@Test
	public void testDeserializeWithNullTtl() throws Exception {
		String json = "{ \"name\": \"foo\", \"ttlMillis\": null }";
		ChannelCreationRequest result = objectMapper.readValue(json, ChannelCreationRequest.class);
		ChannelCreationRequest expected = ChannelCreationRequest.builder().withName("foo").withTtlMillis(null).build();
		assertEquals(expected, result);
	}
}
