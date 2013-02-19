package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.hector.api.Serializer;
import org.apache.thrift.TBaseHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class DataHubCompositeValueSerializer extends AbstractSerializer<DataHubCompositeValue> {

    private final static DataHubCompositeValueSerializer serializer = new DataHubCompositeValueSerializer();
    public static final int BYTES_PER_INT = 4;

    public static Serializer<DataHubCompositeValue> get() {
        return serializer;
    }

    @Override
    public ByteBuffer toByteBuffer(DataHubCompositeValue obj) {

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(calculateBufferLength(obj));

            writeContentType(obj, out);
            writeData(obj, out);

            return ByteBuffer.wrap(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize DataHubCompositeValue: ", e);
        }
    }

    @Override
    public DataHubCompositeValue fromByteBuffer(ByteBuffer byteBuffer) {
        ByteBuffer correctedBuffer = TBaseHelper.rightSize(byteBuffer);
        correctedBuffer.rewind();

        String contentType = readContentType(correctedBuffer);
        byte[] valueData = readValueData(correctedBuffer);

        return new DataHubCompositeValue(contentType, valueData);
    }

    private int calculateBufferLength(DataHubCompositeValue obj) {
        return BYTES_PER_INT + obj.getContentTypeLength() + BYTES_PER_INT + obj.getDataLength();
    }

    private void writeData(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(obj.getDataLength()).array());
        out.write(obj.getData());
    }

    private void writeContentType(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
        int contentTypeLength = obj.getContentTypeLength();
        out.write(ByteBuffer.allocate(4).putInt(contentTypeLength).array());
        if (contentTypeLength > 0) {
            byte[] contentType = safeBytesFromString(obj.getContentType());
            out.write(contentType);
        }
    }

    private String readContentType(ByteBuffer correctedBuffer) {
        byte[] contentTypeBuff = readByteBlock(correctedBuffer);
        return safeStringFromBytes(contentTypeBuff);
    }

    private byte[] readValueData(ByteBuffer correctedBuffer) {
        return readByteBlock(correctedBuffer);
    }

    private byte[] readByteBlock(ByteBuffer correctedBuffer) {
        int dataLength = correctedBuffer.getInt();
        byte[] data = new byte[dataLength];
        correctedBuffer.get(data);
        return data;
    }

    private String safeStringFromBytes(byte[] contentTypeBuffer) {
        try {
            return new String(contentTypeBuffer, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to deserialize due to encoding problem: ", e);
        }
    }

    private byte[] safeBytesFromString(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Unable to serialize data due to encoding type problem: ", e);
        }
    }
}
