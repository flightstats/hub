package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Direct interactions with the file system
 */
@SuppressWarnings("Convert2streamapi")
public class FileSpokeStore {

    private final static Logger logger = LoggerFactory.getLogger(FileSpokeStore.class);

    private final String storagePath;

    @Inject
    public FileSpokeStore(@Named("spoke.path") String storagePath) {
        this.storagePath = StringUtils.appendIfMissing(storagePath, "/");
        logger.info("starting with storage path " + this.storagePath);
        if (!write("hub-startup/" + new ContentKey().toUrl(), ("" + System.currentTimeMillis()).getBytes())) {
            throw new RuntimeException("unable to create startup file");
        }
    }

    public boolean write(String path, byte[] payload) {
        File file = spokeFilePathPart(path);
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
        File file = spokeFilePathPart(path);
        logger.trace("reading {}", file);
        try {
            return FileUtils.readFileToByteArray(file);
        } catch (FileNotFoundException e) {
            logger.info("file not found " + path);
            return null;
        } catch (IOException e) {
            logger.info("unable to read from " + path, e);
            return null;
        }
    }

    public String readKeysInBucket(String path) {
        Collection<String> keys = keysInBucket(path);
        return StringUtils.join(keys, ",");
    }

    public boolean delete(String path) throws Exception {
        FileUtils.deleteDirectory(new File(storagePath + path));
        return true;
    }

    // given a url containing a key, return the file format
    // example: "test_0_4274725520517677/2014/11/18/00/57/24/015/NV2cl5"
    @VisibleForTesting
    File spokeFilePathPart(String urlPathPart) {
        String[] split = StringUtils.split(urlPathPart, "/");
        if (split.length >= 7 && split.length <= 8)
            return new File(storagePath + split[0] + "/" + split[1] + "/" + split[2] + "/" + split[3] + "/" + split[4]
                    + "/" + split[5]);
        if (split.length < 7)
            return new File(storagePath + urlPathPart);
        return new File(storagePath + split[0] + "/" + split[1] + "/" + split[2] + "/" + split[3] + "/" + split[4]
                + "/" + split[5] + "/" + split[6] + split[7] + split[8]);
    }



    //Given a File, return a key part (full key, or time path part)
    String spokeKeyFromPath(String path) {
        if (path.contains(storagePath))
            path = path.substring(storagePath.length());

        // file or directory?
        int i = path.lastIndexOf("/");
        String suffix = path.substring(i + 1);
        if( suffix.length() > 4) {
            // presence of second proves file aims at a full payload path
            String folderPath = path.substring(0, i);
            String seconds = suffix.substring(0, 2);
            String milliseconds = suffix.substring(2, 5);
            String hash = suffix.substring(5);
            return folderPath + "/" + seconds + "/" + milliseconds + "/" + hash;
        }
        // file is a directory
        return path;
    }



    Collection<String> keysInBucket(String key) {
        String path = spokeFilePathPart(key).getAbsolutePath();
        List<String> keys = new ArrayList<>();
        logger.trace("path {}", path);
        String resolution = SpokePathUtil.smallestTimeResolution(key);
        File directory = new File(path);

        if (!directory.exists()) {
            return keys;
        }
        try{
            Collection<File> files;
            if (resolution.equals("second")) {
                // filter all files in the minute folder that start with seconds
                FileFilter fileFilter = new WildcardFileFilter(SpokePathUtil.second(key) + "*");
                files = Arrays.asList(directory.listFiles(fileFilter));
            } else {
                files = FileUtils.listFiles(new File(path), null, true);
            }
            for (File aFile : files) {
                String filePath = aFile.getPath();
                logger.trace("filePath {}", filePath);
                keys.add(spokeKeyFromPath(aFile.getAbsolutePath()));
            }
        } catch (Exception e) {
            logger.info("error with " + path, e);
        }
        return keys;
    }

    public String getLatest(String channel) {
        String last = recurseLatest(channel, 0);
        if (last == null) {
            return null;
        }
        return spokeKeyFromPath(last);
    }

    private String recurseLatest(String path, int count) {
        String base = " ";
        for (String item : new File(storagePath + "/" + path).list()) {
            if (item.compareTo(base) > 0) {
                base = item;
            }
        }
        if (base.equals(" ")) {
            return null;
        }
        logger.trace("count {} base {} path {}", count, base, path);
        if (count == 5) {
            return path + "/" + base;
        }
        count++;
        return recurseLatest(path + "/" + base, count);
    }

}
