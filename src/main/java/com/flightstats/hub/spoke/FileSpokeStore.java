package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
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
        File file = new File(spokeFilePathPart(path));
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
        File file = new File(spokeFilePathPart(path));
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
        Collection<String> keys = keysInBucket(spokeFilePathPart(path));
        return StringUtils.join(keys, ",");
    }

    public boolean delete(String path) throws Exception {
        FileUtils.deleteDirectory(new File(storagePath + path));
        return true;
    }

    // given a url containing a key, return the file format
    // example: "test_0_4274725520517677/2014/11/18/00/57/24/015/NV2cl5"
    @VisibleForTesting
    String spokeFilePathPart(String urlPathPart) {
        //todo - gfm - 11/19/14 - this is confusing
        String[] split = urlPathPart.split("/");
        if (split.length < 9)
            return storagePath + urlPathPart;
        return storagePath + split[0] + "/" + split[1] + "/" + split[2] + "/" + split[3] + "/" + split[4]
                + "/" + split[5] + "/" + split[6] + split[7] + split[8];
    }


    // note: this should only operate on a file path not a url
    String parentOfPath(String path) {
        int index = path.lastIndexOf('/');
        return path.substring(0, index);
    }

    // subtract {storagePath} from spokeRootAndKey
    String keyPart(String spokeRootAndKey) {
        int index = spokeRootAndKey.indexOf(storagePath);
        if (index >= 0) return spokeRootAndKey.substring(storagePath.length());
        return spokeRootAndKey;
    }

    // give me the next path of the resolution of the path passed in.
    // e.g. nextPath( "2014/10/10/22") might return "2014/10/10/23" i.e. the next hour.
    // or it might return "2014/10/11/01" if there was no next in the day 10 bucket.
    // nextPath( "2014/10/10/22/15/hash1") might return "2014/10/10/22/15/hash2" i.e. the next file.
    String adjacentPath(String path, boolean findNext) {
        String spokePath = spokeFilePathPart(path);
        String parentPath = parentOfPath(spokePath);
        File parentFolder = new File(parentPath).getAbsoluteFile();
        File[] files = parentFolder.listFiles((FilenameFilter) null);
        Arrays.sort(files);

        int i = 0;
        // TODO bc 11/13/14: handle the case where the requested item is outside of cache ttl

        i = Arrays.binarySearch(files, new File(spokePath));
        // TODO bc 11/13/14: Make sure we handle the case where there is no next or prev
        // TODO bc 11/14/14: Put these in lambdas that we pass in - when the tech supports it
        String nextPath;
        if (findNext) {
            if (i + 1 < files.length) {
                nextPath = files[i + 1].getAbsoluteFile().toString();
            } else {//   need to get first item of next directory
                String adjacentParent = nextPath(parentPath);
                nextPath = nthFileInFolder(adjacentParent, 0);  //first file
            }
        } else { // find previous
            if (i > 0) {
                nextPath = files[i - 1].getAbsoluteFile().toString();
            } else {//   need to get first item of next directory
                String adjacentParent = previousPath(parentPath);
                nextPath = nthFileInFolder(adjacentParent, -1); //last file
            }
        }
        return spokeKeyFromFilePath(nextPath);
    }

    //Given a filePath, return a key
    String spokeKeyFromFilePath(String filePathPart) {
        int lastSlash = filePathPart.lastIndexOf("/");
        String fileName = filePathPart.substring(lastSlash + 1);
        String folderPath = filePathPart.substring(1, lastSlash);
        if (filePathPart.contains(storagePath))
            folderPath = filePathPart.substring(storagePath.length(), lastSlash);
        String seconds = fileName.substring(0, 2);
        String milliseconds = fileName.substring(2, 5);
        String hash = fileName.substring(5);
        return folderPath + "/" + seconds + "/" + milliseconds + "/" + hash;
    }

    String nthFileInFolder(String path, int index) {
        File file = new File(spokeFilePathPart(path));
        File[] files = file.listFiles((FilenameFilter) null);
        if (index < 0) index = files.length - 1;  // mystery meaning for negative index
        if (files.length < index) {
            return null;
        } else {
            return keyPart(files[index].getAbsolutePath());
        }
    }

    Collection<String> keysInBucket(String path) {
        Collection<File> files;
        List<String> keys = new ArrayList<>();
        logger.trace("path {}", path);
        String resolution = SpokePathUtil.smallestTimeResolution(path);
        // TODO bc 11/21/14: also handle millisecond
        File directory = new File(path);
        if (!directory.exists()) {
            return keys;
        }
        try{
            if(resolution.equals("second")){
                // filter all files in the minute folder that start with seconds
                File dir = (new File(path)).getParentFile();
                FileFilter fileFilter = new WildcardFileFilter(SpokePathUtil.second(path)+"*");
                files = Arrays.asList(dir.listFiles(fileFilter));
            }else {
                files = FileUtils.listFiles(new File(path), null, true);
            }
            Collection<File> files = FileUtils.listFiles(directory, null, true);
            for (File file : files) {
                String filePath = file.getPath();
                logger.trace("filePath {}", filePath);
                keys.add(spokeKeyFromFilePath(filePath));
            }
        } catch (Exception e) {
            logger.info("error with " + path, e);
        }
        return keys;
    }

    Collection<String> adjacentNKeys(String path, int count, boolean next){
        //0: Start with the next item - this will skip to the next hour bucket if need be.
        path = nextPath(path);

        //1: collect all items in current hour adjacent to path
        //  if these >= count, return keys
        String[] adjacentKeys;
        String hourPath = SpokePathUtil.hourPathPart(path);
        Collection<String> hourKeys = keysInBucket(hourPath);
        String[] hourKeyArray = (String []) hourKeys.toArray();
        Arrays.sort(hourKeyArray);
        int i = Arrays.binarySearch(hourKeyArray, new File(path));
        if(next){
            int nextCompliment = hourKeyArray.length - i;
            int to = nextCompliment < count ? i + count: hourKeyArray.length - 1 ;
            adjacentKeys = Arrays.copyOfRange(hourKeyArray,i,to);
        }else{
            int from = i < count ? 0 : i - count ;
            adjacentKeys = Arrays.copyOfRange(hourKeyArray,from,i);
        }
        Collection<String> result = Arrays.asList((String[]) adjacentKeys);
        if (adjacentKeys.length == count)  //terminal
                return result;

        //todo 2: else iterate on adjacent hours until we have enough
        // recurse
        result.addAll(adjacentNKeys(Iterables.getLast(result, null), count - result.size(),
                next));
        return result;
    }

    public Collection<String> nextNKeys(String path, int count){
        return adjacentNKeys(path, count, true);
    }

    public Collection<String> previousNKeys(String path, int count){
        return adjacentNKeys(path, count, false);
    }

    //    public byte[] readNextItem(String path){
//        Path p = Paths.get(spokeFilePathPart(path));
//        Path parent = p.getParent();
//        File parentFolder = new File(parent.toString());
//        String file = p.getFileName().toString();
//        // TODO bc 11/17/14: finish implementation
//        File[] files = parentFolder.listFiles((FileFilter) FileFileFilter.FILE);
//        // find file in listing
//        String nextPath = "";
//        return read(nextPath);
//    }

}
