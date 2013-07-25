package com.flightstats.datahub.dao.serialize;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.google.common.base.Optional;
import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.hector.api.Serializer;
import org.apache.thrift.TBaseHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DataHubCompositeValueSerializer extends AbstractSerializer<DataHubCompositeValue> {

	private final static DataHubCompositeValueSerializer serializer = new DataHubCompositeValueSerializer();
	public static final int BYTES_PER_INT = Integer.SIZE / Byte.SIZE;

	public static Serializer<DataHubCompositeValue> get() {
		return serializer;
	}

	private final OptionalStringSerializer optionalStringSerializer = OptionalStringSerializer.get();
	private final ByteBlockReader byteBlockReader = new ByteBlockReader();

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

		Optional<String> contentType = optionalStringSerializer.fromByteBuffer(correctedBuffer);
		Optional<String> contentEncoding = optionalStringSerializer.fromByteBuffer(correctedBuffer);
		Optional<String> contentLanguage = optionalStringSerializer.fromByteBuffer(correctedBuffer);

		byte[] valueData = readValueData(correctedBuffer);

		return new DataHubCompositeValue(contentType, contentEncoding, contentLanguage, valueData);
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
		out.write(ByteBuffer.allocate(BYTES_PER_INT).putInt(obj.getDataLength()).array());
		out.write(obj.getData());
	}

	private void writeContentType(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
		out.write(optionalStringSerializer.toBytes(obj.getContentType()));
	}

	private void writeContentEncoding(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
		out.write(optionalStringSerializer.toBytes(obj.getContentEncoding()));
	}

	private void writeContentLanguage(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
		out.write(optionalStringSerializer.toBytes(obj.getContentLanguage()));
	}

	private byte[] readValueData(ByteBuffer correctedBuffer) {
		return byteBlockReader.readByteBlock(correctedBuffer);
	}

}
