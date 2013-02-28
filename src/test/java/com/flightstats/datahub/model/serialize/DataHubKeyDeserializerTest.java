package com.flightstats.datahub.model.serialize;

import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import org.codehaus.jackson.JsonParser;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataHubKeyDeserializerTest {

    @Test
    public void testDeserialize() throws Exception {
        Date date = new Date(987654321L);
        short sequence = 54;
        DataHubKey expected = new DataHubKey(date, sequence);
        DataHubKeyRenderer renderer = new DataHubKeyRenderer();

        JsonParser parser = mock(JsonParser.class);

        when(parser.getText()).thenReturn(new DataHubKeyRenderer().keyToString(expected));

        DataHubKeyDeserializer testClass = new DataHubKeyDeserializer(renderer);
        DataHubKey result = testClass.deserialize(parser, null);

        assertEquals(expected, result);
    }
}
