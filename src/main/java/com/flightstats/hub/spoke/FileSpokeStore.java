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

    public String nextPath(String path) {
        return adjacentPath(path, true);
    }

    public String previousPath(String path) {
        return adjacentPath(path, false);
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

    // return the string part before the last "/"
    String getParent(String filePath){
        File file = spokeFilePathPart(filePath);
        return filePath.substring(0, filePath.lastIndexOf("/"));
    }

    // give me the next path of the resolution of the path passed in.
    // e.g. nextPath( "2014/10/10/22") might return "2014/10/10/23" i.e. the next hour.
    // or it might return "2014/10/11/01" if there was no next in the day 10 bucket.
    // nextPath( "2014/10/10/22/15/hash1") might return "2014/10/10/22/15/hash2" i.e. the next file.
    String adjacentPath(String keyPart, boolean findNext) {
        File file = spokeFilePathPart(keyPart);
        File parentFolder = file.getParentFile();
        String parentKey = spokeKeyFromFile(parentFolder);
        File[] files = parentFolder.listFiles();  // immediate children
        if(files==null || files.length ==0) return null;

        Arrays.sort(files);

        int i = Arrays.binarySearch(files, file);
        // TODO bc 11/13/14: Make sure we handle the case where there is no next or prev
        File nextPath;
        if (findNext) {
            if (i + 1 < files.length) {
                nextPath = files[i + 1];
            } else {//   need to get first item of next directory
                File adjacentParent = spokeFilePathPart(nextPath(parentKey));
                nextPath = nthFileInFolder(adjacentParent, 0);  //first file
            }
        } else { // find previous
            if (i > 0) {
                nextPath = files[i - 1];
            } else {//   need to get first item of next directory
                File adjacentParent = spokeFilePathPart(previousPath(parentKey));
                nextPath = nthFileInFolder(adjacentParent, -1); //last file
            }
        }
        return spokeKeyFromFile(nextPath);
    }

    //Given a File, return a key part (full key, or time path part)
    String spokeKeyFromFile(File file) {
        String path = file.getAbsolutePath();
        if (path.contains(storagePath))
            path = file.getAbsolutePath().substring(storagePath.length());

        // file or directory?
        int i = path.lastIndexOf("/");
        String suffix = path.substring(i+1);
        if( suffix.length() > 4) {
            // presence of second proves file aims at a full payload path
            String folderPath = path.substring(0,i);
            String seconds = suffix.substring(0, 2);
            String milliseconds = suffix.substring(2, 5);
            String hash = suffix.substring(5);
            return folderPath + "/" + seconds + "/" + milliseconds + "/" + hash;
        }
        // file is a directory
        return path;
    }

    // given a directory, return the nth item
    // magic negative index gives us the last item in the directory
    File nthFileInFolder(File folder, int index) {
        File[] files = folder.listFiles();
        if(files==null || files.length==0) return null;

        if (index < 0) index = files.length - 1;
        if (files.length < index) {
            return null;
        } else {
            return files[index];
        }
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
                keys.add(spokeKeyFromFile(aFile));
            }
        } catch (Exception e) {
            logger.info("error with " + path, e);
        }
        return keys;
    }

}
