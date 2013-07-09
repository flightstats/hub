package com.flightstats.datahub.dao.serialize;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import me.prettyprint.cassandra.serializers.AbstractSerializer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class OptionalStringSerializer extends AbstractSerializer<Optional<String>> {

	private static final int BYTES_PER_INT = Integer.SIZE / Byte.SIZE;

	private static final OptionalStringSerializer serializer = new OptionalStringSerializer();

	private final ByteBlockReader byteBlockReader = new ByteBlockReader();

	public static OptionalStringSerializer get(){
		return serializer;
	}

	@Override
	public ByteBuffer toByteBuffer(Optional<String> obj) {
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream(getOptionalLength(obj) + 1);
			writeString(out, obj);
			return ByteBuffer.wrap(out.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException("Unable to serialize DataHubCompositeValue: ", e);
		}
	}

	private void writeString(ByteArrayOutputStream out, Optional<String> stringValue) throws IOException {
		int stringLength = getOptionalLength(stringValue);
		out.write(ByteBuffer.allocate(BYTES_PER_INT).putInt(stringLength).array());
		if (stringLength > 0) {
			byte[] bytes = safeBytesFromString(stringValue.get());
			out.write(bytes);
		}
	}

	private int getOptionalLength(Optional<String> stringValue) {
		int stringLength = 0;
		if (stringValue.isPresent()) {
			stringLength = stringValue.get().length();
		}
		return stringLength;
	}

	@Override
	public Optional<String> fromByteBuffer(ByteBuffer byteBuffer) {
		String string = readString(byteBuffer);
		if (Strings.isNullOrEmpty(string)) {
			return Optional.absent();
		}
		return Optional.of(string);
	}


	private String readString(ByteBuffer correctedBuffer) {
		byte[] contentTypeBuff = byteBlockReader.readByteBlock(correctedBuffer);
		return safeStringFromBytes(contentTypeBuff);
	}

		public static String safeStringFromBytes(byte[] contentTypeBuffer) {
		try {
			return new String(contentTypeBuffer, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unable to deserialize due to encoding problem: ", e);
		}
	}

	private static byte[] safeBytesFromString(String string) {
		try {
			return string.getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException("Unable to serialize data due to encoding type problem: ", e);
		}
	}
}
