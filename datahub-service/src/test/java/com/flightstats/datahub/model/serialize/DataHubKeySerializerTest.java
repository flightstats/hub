package com.flightstats.datahub.model.serialize;

import com.fasterxml.jackson.core.JsonGenerator;
import com.flightstats.datahub.model.DataHubKey;
import com.flightstats.datahub.util.DataHubKeyRenderer;
import org.junit.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class DataHubKeySerializerTest {

    @Test
    public void testSerialize() throws Exception {
        DataHubKey key = new DataHubKey(4);
        DataHubKeyRenderer renderer = new DataHubKeyRenderer();

        JsonGenerator jgen = mock(JsonGenerator.class);

        DataHubKeySerializer testClass = new DataHubKeySerializer(renderer);

        testClass.serialize(key, jgen, null);
        verify(jgen).writeString("4");

    }
}
