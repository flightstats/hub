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
        String json = "{\"name\": \"The Name\", \"creationDate\": 808 , \"lastUpdateDate\": 909}";
        ChannelConfiguration result = mapper.readValue(json, ChannelConfiguration.class);
        ChannelConfiguration expected = new ChannelConfiguration("The Name", new Date(808), new Date(909));
        assertEquals(expected, result);
    }
}
