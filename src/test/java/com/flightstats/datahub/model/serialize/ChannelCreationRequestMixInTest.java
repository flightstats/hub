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
    public void testDeserialize() throws Exception {
        String json = "{ \"name\": \"foo\", \"description\": \"bar\"}";
        ChannelCreationRequest result = objectMapper.readValue(json, ChannelCreationRequest.class);
        ChannelCreationRequest expected = new ChannelCreationRequest("foo", "bar");
        assertEquals(expected, result);
    }

    @Test
    public void testDescriptionIsOptional() throws Exception {
        String json = "{ \"name\": \"foo\" }";
        ChannelCreationRequest result = objectMapper.readValue(json, ChannelCreationRequest.class);
        ChannelCreationRequest expected = new ChannelCreationRequest("foo", null);
        assertEquals(expected, result);
    }


}
