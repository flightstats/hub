package com.flightstats.hub.spoke;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * Direct interactions with the file system
 */
public class SpokeFileStore {

    private final static Logger logger = LoggerFactory.getLogger(SpokeFileStore.class);

    private final String storagePath;

    public SpokeFileStore(String storagePath) {
        this.storagePath = storagePath;
        logger.info("starting with storage path " + storagePath);
    }

    public boolean write(String path, byte[] payload) {
        File file = new File(storagePath + "/" + path);
        logger.trace("writing {}", file);
        try {
            FileUtils.writeByteArrayToFile(file, payload);
        } catch (IOException e) {
            logger.warn("unable to write to " + path, e);
            return false;
        }
        return true;
    }

    public byte[] read(String path) {
        File file = new File(storagePath + "/" + path);
        logger.trace("reading {}", file);
        try {
            return FileUtils.readFileToByteArray(file);
        } catch (IOException e) {
            logger.warn("unable to read from " + path, e);
            return null;
        }
    }

    /*
    todo - gfm - 11/12/14 -
    public Collection<ContentKey> getKeys(String channelName, long startMillis, long endMillis) {
        return null;
    }

    public Collection<ContentKey> getKeys(String channelName, ContentKey contentKey, int count) {
        return null;
    }

    todo - gfm - 11/12/14 - add delete to support TTL

    public void delete(String channelName) {

    }
    */
}
