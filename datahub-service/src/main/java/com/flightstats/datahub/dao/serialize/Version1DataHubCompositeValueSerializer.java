package com.flightstats.datahub.dao.serialize;

import com.flightstats.datahub.model.DataHubCompositeValue;
import com.google.common.base.Optional;
import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.cassandra.serializers.LongSerializer;
import me.prettyprint.hector.api.exceptions.HectorSerializationException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class Version1DataHubCompositeValueSerializer extends AbstractSerializer<DataHubCompositeValue> {

    static final byte FORMAT_VERSION_01 = 0x01;

    private final ByteBlockReader byteBlockReader = new ByteBlockReader();
    private final OptionalStringSerializer optionalStringSerializer = OptionalStringSerializer.get();
    private final LongSerializer longSerializer = new LongSerializer();

    @Override
    public DataHubCompositeValue fromByteBuffer(ByteBuffer byteBuffer) {

        Optional<String> contentType = Optional.absent();
        Optional<String> contentLanguage = Optional.absent();
        byte[] valueData = new byte[]{};
        long millis = 0;

        byte version = byteBuffer.get();
        if(version != FORMAT_VERSION_01){
            throw new HectorSerializationException("Unhandled version (" + version + " but expected " + FORMAT_VERSION_01);
        }
        while(byteBuffer.hasRemaining()){
            FieldId fieldId = getFieldId(byteBuffer);
            if(fieldId == null){
                byteBlockReader.readByteBlock(byteBuffer); //consume field and ignore content
                continue;
            }
            switch(fieldId){
                case CONTENT_TYPE:
                    contentType = optionalStringSerializer.fromByteBuffer(byteBuffer);
                    break;
                case CONTENT_LANGUAGE:
                    contentLanguage = optionalStringSerializer.fromByteBuffer(byteBuffer);
                    break;
                case CONTENT:
                    valueData = byteBlockReader.readByteBlock(byteBuffer);
                    break;
                case MILLIS:
                    millis = longSerializer.fromByteBuffer(byteBuffer);
                    break;

            }
        }
        return new DataHubCompositeValue(contentType, contentLanguage, valueData, millis);
    }

    @Override
    public ByteBuffer toByteBuffer(DataHubCompositeValue obj) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(FORMAT_VERSION_01);
            writeContentType(obj, out);
            writeContentLanguage(obj, out);
            writeContent(obj, out);
            writeLong(out, FieldId.MILLIS, obj.getMillis());
            return ByteBuffer.wrap(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Unable to serialize DataHubCompositeValue: ", e);
        }
    }

    private FieldId getFieldId(ByteBuffer byteBuffer) {
        try {
            return FieldId.fromInt(byteBuffer.getInt());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void writeContentType(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
        writeOptionalString(out, FieldId.CONTENT_TYPE, obj.getContentType());
	}

    private void writeContentLanguage(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
		writeOptionalString(out, FieldId.CONTENT_LANGUAGE, obj.getContentLanguage());
	}

    private void writeContent(DataHubCompositeValue obj, ByteArrayOutputStream out) throws IOException {
        writeInt(out, FieldId.CONTENT.getId());
        writeInt(out, obj.getDataLength());
        out.write(obj.getData());
    }

    private void writeOptionalString(ByteArrayOutputStream out, FieldId fieldId, Optional<String> optionalString) throws IOException {
        writeInt(out, fieldId.getId());
        out.write(optionalStringSerializer.toBytes(optionalString));
    }

    private void writeInt(ByteArrayOutputStream out, int id) throws IOException {
        out.write(ByteBuffer.allocate(DataHubCompositeValueSerializer.BYTES_PER_INT).putInt(id).array());
    }

    private void writeLong(ByteArrayOutputStream out, FieldId fieldId, long longValue) throws IOException {
        writeInt(out, fieldId.getId());
        out.write(longSerializer.toBytes(longValue));
    }
}
