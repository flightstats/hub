package com.flightstats.datahub.dao;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.google.common.base.Optional;
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
            writeContentEncoding(obj, out);
            writeContentLanguage(obj, out);
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

        String contentType = readString(correctedBuffer);
        String contentEncoding = readString(correctedBuffer);
        String contentLanguage = readString(correctedBuffer);

        byte[] valueData = readValueData(correctedBuffer);

        return new DataHubCompositeValue(Optional.of(contentType), Optional.of(contentEncoding), Optional.of(contentLanguage), valueData);
    }

    private int calculateBufferLength(DataHubCompositeValue value) {
        return BYTES_PER_INT + optionalLength(value.getContentType()) +
				BYTES_PER_INT + optionalLength(value.getContentEncoding()) +
				BYTES_PER_INT + optionalLength(value.getContentLanguage()) +
				BYTES_PER_INT + value.getDataLength();
    }

	private int optionalLength(Optional<String> contentType) {
		return contentType.isPresent() ? contentType.get().length() : 0;
	}

	private void writeData(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
        out.write(ByteBuffer.allocate(4).putInt(obj.getDataLength()).array());
        out.write(obj.getData());
    }

    private void writeContentType(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
		writeString(out, obj.getContentType());
    }

	private void writeContentEncoding(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
		writeString(out, obj.getContentEncoding());
	}

	private void writeContentLanguage(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
		writeString(out, obj.getContentLanguage());
	}

	private void writeString(ByteArrayOutputStream out, Optional<String> stringValue) throws IOException {
		int stringLength = 0;
		if(stringValue.isPresent()){
			stringLength = stringValue.get().length();
		}
		out.write(ByteBuffer.allocate(4).putInt(stringLength).array());
		if (stringLength > 0) {
			byte[] bytes = safeBytesFromString(stringValue.get());
            out.write(bytes);
        }
	}

	private String readString(ByteBuffer correctedBuffer) {
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
