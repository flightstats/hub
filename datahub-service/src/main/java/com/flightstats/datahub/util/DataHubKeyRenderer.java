package com.flightstats.datahub.util;

import com.flightstats.datahub.model.DataHubKey;
import org.apache.commons.codec.binary.Base32;

import java.io.*;

public class DataHubKeyRenderer {

    public String keyToString(DataHubKey key) {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream(8);
        DataOutputStream out = new DataOutputStream(byteOut);
        writeBytesSafely(key, out);
        Base32 base32 = new Base32(true);
        byte[] bytes = byteOut.toByteArray();
        return base32.encodeAsString(bytes);
    }

    public DataHubKey fromString(String key) {
        byte[] bytes = new Base32(true).decode(key);
        ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
        DataInputStream in = new DataInputStream(bytesIn);
        try {
            return new DataHubKey(in.readLong());
        } catch (IOException e) {
            throw new RuntimeException("Error converting data hub key", e);
        }
    }

    private void writeBytesSafely(DataHubKey key, DataOutputStream out) {
        try {
            out.writeLong(key.getSequence());
        } catch (IOException e) {
            throw new RuntimeException("Error converting long to bytes", e);
        }
    }

}
