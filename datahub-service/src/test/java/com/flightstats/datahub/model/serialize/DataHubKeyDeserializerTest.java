package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.core.JsonParser;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DataHubKeyDeserializerTest {

    @Test
    public void testDeserialize() throws Exception {

        short sequence = 1054;
        DataHubKey expected = new DataHubKey(sequence);
        DataHubKeyRenderer renderer = new DataHubKeyRenderer();

        JsonParser parser = mock(JsonParser.class);

        when(parser.getText()).thenReturn(new DataHubKeyRenderer().keyToString(expected));

        DataHubKeyDeserializer testClass = new DataHubKeyDeserializer(renderer);
        DataHubKey result = testClass.deserialize(parser, null);

        assertEquals(expected, result);
    }
}
