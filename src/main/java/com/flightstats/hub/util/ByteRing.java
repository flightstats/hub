package com.flightstats.hub.util;

import java.util.Arrays;

public class ByteRing {

    private byte[] buffer;
    private int position = 0;

    public ByteRing(int size) {
        buffer = new byte[size];
        Arrays.fill(buffer, (byte) -1);
    }

    public void put(byte b) {
        if (position == buffer.length) {
            position = 0;
        }
        buffer[position] = b;
        position++;
    }

    /**
     * Compare the given array to the end of this ring.
     *
     * @param given
     * @return true if the given array matches the end of this ring
     */
    public boolean compare(byte[] given) {
        int start = position - given.length - 1;
        if (start < 0) {
            start = buffer.length + start;
        }
        for (int i = 0; i < given.length; i++) {
            start++;
            if (start >= buffer.length) {
                start = 0;
            }
            if (given[i] != buffer[start]) {
                return false;
            }
        }
        return true;
    }

    byte[] getBuffer() {
        return buffer;
    }
}
