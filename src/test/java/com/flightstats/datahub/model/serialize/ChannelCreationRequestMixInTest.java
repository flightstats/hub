package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.app.config.DataHubContextResolver;
import com.flightstats.datahub.model.ChannelCreationRequest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ChannelCreationRequestMixInTest {

    @Test
    public void testDeserialize() throws Exception {
        String json = "{ \"name\": \"foo\", \"description\": \"bar\"}";
        DataHubContextResolver resolver = new DataHubContextResolver();
        ChannelCreationRequest result = resolver.getContext(ChannelCreationRequest.class).readValue(json, ChannelCreationRequest.class);
        ChannelCreationRequest expected = new ChannelCreationRequest("foo", "bar");
        assertEquals(expected, result);
    }

    @Test
    public void testDescriptionIsOptional() throws Exception {
        String json = "{ \"name\": \"foo\" }";
        DataHubContextResolver resolver = new DataHubContextResolver();
        ChannelCreationRequest result = resolver.getContext(ChannelCreationRequest.class).readValue(json, ChannelCreationRequest.class);
        ChannelCreationRequest expected = new ChannelCreationRequest("foo", null);
        assertEquals(expected, result);
    }


}
