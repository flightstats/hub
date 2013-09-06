package com.flightstats.datahub.dao.serialize;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.google.common.base.Optional;
import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.exceptions.HectorSerializationException;
import org.apache.thrift.TBaseHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class DataHubCompositeValueSerializer extends AbstractSerializer<DataHubCompositeValue> {

	private final static DataHubCompositeValueSerializer serializer = new DataHubCompositeValueSerializer();
	public static final int BYTES_PER_INT = Integer.SIZE / Byte.SIZE;
	static final byte FORMAT_VERSION = 66;

	public static Serializer<DataHubCompositeValue> get() {
		return serializer;
	}

	private final OptionalStringSerializer optionalStringSerializer = OptionalStringSerializer.get();
	private final ByteBlockReader byteBlockReader = new ByteBlockReader();

    /**
     * This format:
     *
     * vaaaaxxxx[d1]aaaaxxxx[d2]
     *
     * where:  v      the version byte
     *         aaaa   the 4-byte field id (int)
     *         xxxx   the 4-byte field length (int)
     *         [d1]   the data for the field, variable length
     *
     */
    @Override
    public DataHubCompositeValue fromByteBuffer(ByteBuffer byteBuffer) {
        ByteBuffer correctedBuffer = TBaseHelper.rightSize(byteBuffer);
        correctedBuffer.rewind();

        byte versionByte = byteBuffer.get();
        //Note: In the future, we could dispatch to an appropriate version reader if/when this changes.
        if(versionByte != FORMAT_VERSION){
            throw new HectorSerializationException("Unrecognized format version: " + versionByte);
        }
        return deserializeCompositeValue(correctedBuffer);
    }

    private DataHubCompositeValue deserializeCompositeValue(ByteBuffer correctedBuffer) {
        Optional<String> contentType = Optional.absent();
        Optional<String> contentLanguage = Optional.absent();
        byte[] valueData = new byte[]{};

        while(correctedBuffer.hasRemaining()){
            FieldId fieldId = getFieldId(correctedBuffer);
            if(fieldId == null){
                byteBlockReader.readByteBlock(correctedBuffer); //consume field and ignore content
                continue;
            }
            switch(fieldId){
                case CONTENT_TYPE:
                    contentType = optionalStringSerializer.fromByteBuffer(correctedBuffer);
                    break;
                case CONTENT_LANGUAGE:
                    contentLanguage = optionalStringSerializer.fromByteBuffer(correctedBuffer);
                    break;
                case CONTENT:
                    valueData = byteBlockReader.readByteBlock(correctedBuffer);
                    break;
            }
        }
        return new DataHubCompositeValue(contentType, contentLanguage, valueData);
    }

    private FieldId getFieldId(ByteBuffer byteBuffer) {
        try {
            return FieldId.fromInt(byteBuffer.getInt());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    @Override
	public ByteBuffer toByteBuffer(DataHubCompositeValue obj) {

		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(FORMAT_VERSION);
			writeContentType(obj, out);
			writeContentLanguage(obj, out);
			writeContent(obj, out);
			return ByteBuffer.wrap(out.toByteArray());
		} catch (IOException e) {
			throw new RuntimeException("Unable to serialize DataHubCompositeValue: ", e);
		}
	}

	private void writeContentType(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
        writeOptionalString(out, FieldId.CONTENT_TYPE, obj.getContentType());
	}

    private void writeContentLanguage(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
		writeOptionalString(out, FieldId.CONTENT_LANGUAGE, obj.getContentLanguage());
	}

    private void writeOptionalString(ByteArrayOutputStream out, FieldId fieldId, Optional<String> optionalString) throws IOException {
        writeInt(out, fieldId.getId());
        out.write(optionalStringSerializer.toBytes(optionalString));
    }

    private void writeContent(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
        writeInt(out, FieldId.CONTENT.getId());
        writeInt(out, obj.getDataLength());
        out.write(obj.getData());
    }

    private void writeInt(ByteArrayOutputStream out, int id) throws IOException {
        out.write(ByteBuffer.allocate(BYTES_PER_INT).putInt(id).array());
    }
}
