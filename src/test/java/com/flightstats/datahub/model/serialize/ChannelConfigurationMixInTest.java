package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.DataHubKey;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;

public class ChannelConfigurationMixInTest {

    @Test
    public void test() throws Exception {
        ObjectMapper mapper = new DataHubObjectMapperFactory().build();
        DataHubKey lastUpdateKey = new DataHubKey(new Date(909), (short) 520);

        String json = "{\"name\": \"The Name\", \"creationDate\": 808 , \"lastUpdateKey\": \"00000000001OQ0G8\"}";
        ChannelConfiguration result = mapper.readValue(json, ChannelConfiguration.class);
        ChannelConfiguration expected = new ChannelConfiguration("The Name", new Date(808), lastUpdateKey);
        assertEquals(expected, result);
    }
}
