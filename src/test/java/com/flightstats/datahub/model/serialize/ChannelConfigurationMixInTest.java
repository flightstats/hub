package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.ChannelConfiguration;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertEquals;

public class ChannelConfigurationMixInTest {

    @Test
    public void test() throws Exception {
        ObjectMapper mapper = new DataHubObjectMapperFactory().build();
        String json = "{\"name\": \"The Name\", \"description\": \"The way of the light\", \"creationDate\": 808 }";
        ChannelConfiguration result = mapper.readValue(json, ChannelConfiguration.class);
        ChannelConfiguration expected = new ChannelConfiguration("The Name", "The way of the light", new Date(808));
        assertEquals(expected, result);
    }
}
