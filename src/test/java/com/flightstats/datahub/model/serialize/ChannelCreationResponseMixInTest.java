package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.app.config.DataHubContextResolver;
import com.flightstats.datahub.model.ChannelConfiguration;
import com.flightstats.datahub.model.ChannelCreationResponse;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Date;

import static junit.framework.Assert.assertEquals;

public class ChannelCreationResponseMixInTest {

    @Test
    public void testSerialize() throws Exception {
        ObjectMapper mapper = new DataHubContextResolver().getContext(ChannelCreationResponse.class);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Date date = new Date();
        ChannelConfiguration channelConfiguration = new ChannelConfiguration("supachunka", "one fat pipe", date);
        URI channelUri = new URI("http://path/to/something");
        URI latestUri = new URI("http://path/to/something/latest");
        ChannelCreationResponse response = new ChannelCreationResponse(channelUri, latestUri, channelConfiguration);
        mapper.writeValue(out, response);
        String expected = "{\n" +
                "\"_links\": {\n" +
                "    \"self\": { \"href\": \"http://path.to/supachunka\"},\n" +
                "    \"latest\": { \"href\": \"http://path.to/supachunka/latest\"}  \n" +
                "},\n" +
                "\"name\": \"supachunka\",\n" +
                "\"description\": \"one fat pipe\",\n" +
                "}";
        assertEquals(expected, out.toString());
    }
}
