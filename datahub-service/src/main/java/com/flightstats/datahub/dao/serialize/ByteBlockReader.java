package com.flightstats.datahub.dao.serialize;

import java.nio.ByteBuffer;

public class ByteBlockReader {

	public byte[] readByteBlock(ByteBuffer correctedBuffer) {
		int dataLength = correctedBuffer.getInt();
		byte[] data = new byte[dataLength];
		correctedBuffer.get(data);
		return data;
	}
}
