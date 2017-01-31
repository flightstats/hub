package com.flightstats.hub.util;

class ChunkStrategy {

    private static final int MEGABYTES = 1024 * 1024;

    static int getSize(int count) {
        int factor = 160;
        if (count <= 3) {
            factor = 5;
        } else if (count <= 6) {
            factor = 10;
        } else if (count <= 9) {
            factor = 20;
        } else if (count <= 12) {
            factor = 40;
        } else if (count <= 15) {
            factor = 80;
        }
        return factor * MEGABYTES;
    }
}
