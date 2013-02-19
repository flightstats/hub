package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.app.config.DataHubObjectMapperFactory;
import com.flightstats.datahub.model.ValueInsertionResult;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;

public class ValueInsertionResultMixInTest {

    @Test
    public void testSerialize() throws Exception {
        ObjectMapper objectMapper = new DataHubObjectMapperFactory().build();
        UUID id = new UUID(42L, 43L);
        ValueInsertionResult valueInsertionResult = new ValueInsertionResult(id, new Date(1123456678922L));
        OutputStream out = new ByteArrayOutputStream();
        objectMapper.writeValue(out, valueInsertionResult);
        String result = out.toString();
        assertEquals("{\n" +
                "  \"id\" : \"00000000-0000-002a-0000-00000000002b\",\n" +
                "  \"timestamp\" : \"2005-08-07T16:17:58.922-07:00\"\n" +
                "}", result);
    }
}
