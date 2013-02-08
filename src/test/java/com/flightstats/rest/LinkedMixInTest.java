package com.flightstats.rest;

import com.flightstats.datahub.app.config.DataHubContextResolver;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class LinkedMixInTest {

    @Test
    public void testSerialize() throws Exception {
        ObjectMapper mapper = new DataHubContextResolver().getContext(LinkedMixIn.class);
        URI link1 = URI.create("http://path/to/previous");
        URI link2 = URI.create("http://path/to/next");
        Map<String, String> map = new HashMap<>();
        map.put("foo", "bar");
        Linked<Map<String, String>> testClass = Linked.linked(map)
                                                      .withLink("previous", link1)
                                                      .withLink("next", link2)
                                                      .build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        mapper.writeValue(out, testClass);
        String expected = "{\n" +
                "  \"_links\" : {\n" +
                "    \"previous\" : {\n" +
                "      \"href\" : \"http://path/to/previous\"\n" +
                "    },\n" +
                "    \"next\" : {\n" +
                "      \"href\" : \"http://path/to/next\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"object\" : {\n" +
                "    \"foo\" : \"bar\"\n" +
                "  }\n" +
                "}";
        assertEquals(expected, out.toString());
    }

}
