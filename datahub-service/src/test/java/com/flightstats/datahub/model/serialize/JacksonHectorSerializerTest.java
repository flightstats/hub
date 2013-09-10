package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class JacksonHectorSerializerTest {

    @Test
    public void testRoundTrip() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JacksonHectorSerializer<List> testClass = new JacksonHectorSerializer<>(mapper, List.class);
        List<String> input = Arrays.asList("one", "two");
        ByteBuffer buffer = testClass.toByteBuffer(input);  //serialize
        List result = testClass.fromByteBuffer(buffer);     //deserialize
        assertEquals("It should be the same", result, input);
    }
}
