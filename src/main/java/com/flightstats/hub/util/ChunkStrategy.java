package com.flightstats.hub.util;

import com.flightstats.hub.app.HubProperties;

class ChunkStrategy {

    private static final int MEGABYTES = 1024 * 1024;

    private static int getMaxChunk() {
        return HubProperties.getProperty("s3.maxChunkMB", 40);
    }

    static int getSize(int count) {
        int chunkMB = (Math.floorDiv(count, 3) + 1) * 5;
        return Math.min(chunkMB, getMaxChunk()) * MEGABYTES;
    }
}
