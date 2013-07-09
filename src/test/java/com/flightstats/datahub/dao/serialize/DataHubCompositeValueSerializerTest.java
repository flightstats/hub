package com.flightstats.datahub.dao.serialize;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.google.common.base.Optional;
import me.prettyprint.hector.api.Serializer;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class DataHubCompositeValueSerializerTest {

    @Test
    public void testRoundTrip() throws Exception {
        Serializer<DataHubCompositeValue> testClass = DataHubCompositeValueSerializer.get();
        DataHubCompositeValue instance = new DataHubCompositeValue(Optional.of("text/plain"), Optional.of("gzip"), Optional.of("en, mi"), "any arbitrary massage".getBytes());
        ByteBuffer byteBuffer = testClass.toByteBuffer(instance);
        DataHubCompositeValue result = testClass.fromByteBuffer(byteBuffer);
        assertEquals(result, instance);
    }

}
