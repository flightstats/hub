package com.flightstats.rest;

import me.prettyprint.cassandra.serializers.AbstractSerializer;
import org.apache.thrift.TBaseHelper;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This class is a generic serializer/deserializer that introduces Jackson to Hector.
 * It groks JSON.
 */
public class JacksonHectorSerializer<T> extends AbstractSerializer<T> {

    private final ObjectMapper objectMapper;
    private final Class<T> clazz;

    public JacksonHectorSerializer(ObjectMapper objectMapper, Class<T> clazz) {
        this.objectMapper = objectMapper;
        this.clazz = clazz;
    }

    @Override
    public ByteBuffer toByteBuffer(T obj) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            objectMapper.writeValue(out, obj);
            return ByteBuffer.wrap(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Cannot seem to serialize an AirQualityEgg.", e);
        }
    }

    @Override
    public T fromByteBuffer(ByteBuffer byteBuffer) {
        try {
            ByteBuffer correctedBuffer = TBaseHelper.rightSize(byteBuffer);
            byte[] buffer = new byte[correctedBuffer.remaining()];
            correctedBuffer.get(buffer);
            return objectMapper.readValue(buffer, clazz);
        } catch (IOException e) {
            throw new RuntimeException("Error serializing to AirQualityEgg.", e);
        }
    }
}
