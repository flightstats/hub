package com.flightstats.hub.util;

public class Chunk {

    private int count;
    private int chunkSize;
    private byte[] bytes;
    private int index = 0;

    Chunk(int count, int chunkSize) {
        this.count = count;
        this.chunkSize = chunkSize;
        bytes = new byte[chunkSize];
    }

    /**
     * @param b byte to write
     * @return false if the byte is not added.
     */
    boolean add(int b) {
        if (isFull()) {
            return false;
        }
        bytes[index] = (byte) b;
        index++;
        return true;
    }

    boolean isFull() {
        return index >= chunkSize;
    }

    boolean hasData() {
        return index > 0;
    }

    public byte[] getBytes() {
        if (isFull()) {
            return bytes;
        }
        byte[] partial = new byte[index];
        System.arraycopy(this.bytes, 0, partial, 0, index);
        return partial;
    }

    public int getCount() {
        return count;
    }
}