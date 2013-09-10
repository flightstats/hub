package com.flightstats.datahub.dao.serialize;

import com.flightstats.datahub.model.DataHubCompositeValue;
import me.prettyprint.cassandra.serializers.AbstractSerializer;
import me.prettyprint.hector.api.Serializer;
import me.prettyprint.hector.api.exceptions.HectorSerializationException;
import org.apache.thrift.TBaseHelper;

import java.nio.ByteBuffer;

import static com.flightstats.datahub.dao.serialize.Version1DataHubCompositeValueSerializer.FORMAT_VERSION_01;

public class DataHubCompositeValueSerializer extends AbstractSerializer<DataHubCompositeValue> {

	private final static DataHubCompositeValueSerializer serializer = new DataHubCompositeValueSerializer();
	public static final int BYTES_PER_INT = Integer.SIZE / Byte.SIZE;

    private final Version1DataHubCompositeValueSerializer version1Serializer = new Version1DataHubCompositeValueSerializer();
    private final byte CURRENT_VERSION = Version1DataHubCompositeValueSerializer.FORMAT_VERSION_01;

    public static Serializer<DataHubCompositeValue> get() {
		return serializer;
	}

    /**
     * This is the version 1 format:
     *
     * vaaaaxxxx[d1]aaaaxxxx[d2]
     *
     * where:  v      the version byte
     *         aaaa   the 4-byte field id (int)
     *         xxxx   the 4-byte field length (int)
     *         [d1]   the data for the field, variable length
     *
     * Future formats must keep the version number in the first byte for continued extensibility     *
     */
    @Override
    public DataHubCompositeValue fromByteBuffer(ByteBuffer byteBuffer) {
        ByteBuffer correctedBuffer = TBaseHelper.rightSize(byteBuffer);
        correctedBuffer.rewind();

        byte versionByte = byteBuffer.array()[0];
        switch(versionByte){
            case FORMAT_VERSION_01:
                return version1Serializer.fromByteBuffer(correctedBuffer);
            default:
                throw new HectorSerializationException("Unrecognized format version: " + versionByte);
        }
    }

    @Override
	public ByteBuffer toByteBuffer(DataHubCompositeValue obj) {
        switch(CURRENT_VERSION){
            case FORMAT_VERSION_01:
                return version1Serializer.toByteBuffer(obj);
            default:
                throw new IllegalStateException("Unable to serialize version " + CURRENT_VERSION);
        }
    }

}
