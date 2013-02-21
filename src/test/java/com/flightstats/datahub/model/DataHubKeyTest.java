package com.flightstats.datahub.model;

import org.apache.commons.codec.binary.Base32;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertEquals;

public class DataHubKeyTest {

    @Test
    public void testAsSortableString() throws Exception {
        DataHubKey testClass = new DataHubKey(new Date(1361404433284L), 44);
        assertEquals("2013-02-20T05:59:45.123.000044", testClass.asSortableString());
    }

    @Test
    public void testFromSortableString() throws Exception {
        fail("Build me.");
    }

    @Test
    public void testFromSortableString_invalidFormat() throws Exception {
        fail("Build me.");
    }

    @Test
    public void testWhatWouldItLookLike() throws Exception {

        for (int i = 0; i < 1000; i++) {
            System.out.println(enc(System.currentTimeMillis()));
            Thread.sleep(300);
        }
    }

    String enc(long v) throws IOException {
        ByteArrayOutputStream byteOut = new ByteArrayOutputStream(8);
        DataOutputStream out = new DataOutputStream(byteOut);
        try {
            out.writeLong(v);
        } catch (IOException e) {
            throw new RuntimeException("Error converting long to bytes", e);
        }
        out.write((byte) 0);
        byte[] bytes = byteOut.toByteArray();
        new Base32()
        byte[] base64encoded = Base32.encode(bytes);
        String result = new String(base64encoded);
        return result;
    }
}
