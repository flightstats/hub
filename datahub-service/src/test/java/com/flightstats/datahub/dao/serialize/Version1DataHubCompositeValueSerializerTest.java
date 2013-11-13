package com.flightstats.datahub.dao.serialize;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.google.common.base.Optional;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.exceptions.HectorSerializationException;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;

public class Version1DataHubCompositeValueSerializerTest {

    @Test
    public void testRoundTrip() throws Exception {
        Serializer<DataHubCompositeValue> testClass = new Version1DataHubCompositeValueSerializer();
        DataHubCompositeValue instance = new DataHubCompositeValue(Optional.of("text/plain"), Optional.of("en, mi"), "any arbitrary massage".getBytes(),
                0L);
        ByteBuffer byteBuffer = testClass.toByteBuffer(instance);
        DataHubCompositeValue result = testClass.fromByteBuffer(byteBuffer);
        assertEquals(result, instance);
    }

    @Test
    public void testFromBytes_UnknownField() throws Exception {
        Serializer<DataHubCompositeValue> testClass = new Version1DataHubCompositeValueSerializer();
        DataHubCompositeValue instance = new DataHubCompositeValue(Optional.of("nomatta"), Optional.of("en, mi"), "God lived as a devil dog.".getBytes(),
                0L);
        ByteBuffer byteBuffer = testClass.toByteBuffer(instance);
        byte[] bytes = byteBuffer.array();
        //Stomp over the first field, which is currently the content type.  It's at offset 1 (0th byte is version)
        System.arraycopy(new byte[]{0, 0, 1, 2}, 0, bytes, 1, 4);

        DataHubCompositeValue result = testClass.fromBytes(bytes);
        DataHubCompositeValue expected = new DataHubCompositeValue(Optional.<String>absent(), instance.getContentLanguage(), instance.getData(), 0L);
        assertEquals(expected, result);
    }

    @Test
    public void testFromBytes_MissingField() throws Exception {
        Serializer<DataHubCompositeValue> testClass = new Version1DataHubCompositeValueSerializer();
        DataHubCompositeValue instance = new DataHubCompositeValue(Optional.of("a"), Optional.of("b"), "c".getBytes(), 0L);
        ByteBuffer byteBuffer = testClass.toByteBuffer(instance);
        byte[] bytes = byteBuffer.array();
        byte[] newBytes = new byte[bytes.length - 9];   // 9 = 4-byte int(field id) + 4-byte int(length) + 1 byte "a" string
        newBytes[0] = bytes[0];
        // Skip bytes 0-9 (inclusive)
        System.arraycopy(bytes, 10, newBytes, 1, bytes.length - (4 + 4 + 1) - 1);

        DataHubCompositeValue result = testClass.fromBytes(newBytes);
        DataHubCompositeValue expected = new DataHubCompositeValue(Optional.<String>absent(), instance.getContentLanguage(), instance.getData(), 0L);
        assertEquals(expected, result);
    }

    @Test(expected = HectorSerializationException.class)
    public void testVersionMismatch() throws Exception {
        Serializer<DataHubCompositeValue> testClass = new Version1DataHubCompositeValueSerializer();
        DataHubCompositeValue instance = new DataHubCompositeValue(Optional.of("text/plain"), Optional.of("en, mi"), "any arbitrary massage".getBytes(),
                0L);
        ByteBuffer byteBuffer = testClass.toByteBuffer(instance);
        byteBuffer.array()[0] = 0x07;
        testClass.fromByteBuffer(byteBuffer);
    }

}
