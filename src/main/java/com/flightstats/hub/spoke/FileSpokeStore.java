package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Vector;

/**
 * Direct interactions with the file system
 */
public class FileSpokeStore {

    private final static Logger logger = LoggerFactory.getLogger(FileSpokeStore.class);

    private final String storagePath;

    @Inject
    public FileSpokeStore(@Named("spoke.path") String storagePath) {
        this.storagePath = StringUtils.appendIfMissing(storagePath, "/");
        logger.info("starting with storage path " + this.storagePath);
        if (!write("!startup/" + new ContentKey().toUrl(), ("" + System.currentTimeMillis()).getBytes())) {
            throw new RuntimeException("unable to create startup file");
        }
    }

    @VisibleForTesting
    String spokePath(String path) {
        String[] split = path.split("/");
        logger.info("split " + Arrays.toString(split));
        return storagePath + split[0] + "/" + split[1] + "/" + split[2] + "/" + split[3] + "/" + split[4]
                + "/" + split[5] + "/" + split[6] + split[7] + split[8];
    }

    public boolean write(String path, byte[] payload) {
        File file = new File(spokePath(path));
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
        File file = new File(spokePath(path));
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




    public String parentOfPath(String path){
        int index=path.lastIndexOf('/');
        return path.substring(0,index);
    }

    public String keyPart(String spokeRootAndKey){
        int index=spokeRootAndKey.indexOf(storagePath);
        if(index>=0) return spokeRootAndKey.substring(storagePath.length());
        return spokeRootAndKey;
    }

    // give me the next path of the resolution of the path passed in.
    // e.g. nextPath( "2014/10/10/22") might return "2014/10/10/23" i.e. the next hour.
    // or it might return "2014/10/11/01" if there was no next in the day 10 bucket.
    // nextPath( "2014/10/10/22/15/hash1") might return "2014/10/10/22/15/hash2" i.e. the next file.
    public String adjacentPath(String path, boolean findNext){
        String parentPath = parentOfPath(path);
        String spokePath = spokePath(path);
        File parentFolder = new File(spokePath(parentPath)).getAbsoluteFile();
        File[] files = parentFolder.listFiles((FilenameFilter) null);
        Arrays.sort(files);

        int i = 0;
        // TODO bc 11/13/14: handle the case where the requested item is outside of cache ttl
        for(; i < files.length; i++){
            if(files[i].getAbsolutePath().equals(spokePath)){
                break;
            }
        }
        // TODO bc 11/13/14: Make sure we handle the case where there is no next or prev
        // TODO bc 11/14/14: Put these in lambdas that we pass in - when the tech supports it
        String nextPath;
        if(findNext) {
            if (i + 1 < files.length) {
                nextPath = files[i + 1].getAbsoluteFile().toString();
            } else {//   need to get first item of next directory
                String adjacentParent = nextPath(parentPath);
                nextPath = nthFileInFolder(adjacentParent, 0);  //first file
            }
        }else { // find previous
            if (i  > 0) {
                nextPath = files[i - 1].getAbsoluteFile().toString();
            } else {//   need to get first item of next directory
                String adjacentParent = previousPath(parentPath);
                nextPath = nthFileInFolder(adjacentParent, -1); //last file
            }
        }
        return keyPart(nextPath);
    }

    // sugar
    public String nextPath(String path){
        return adjacentPath(path, true);
    }

    public String previousPath(String path){
        return adjacentPath(path, false);
    }

    public String nthFileInFolder(String path, int index){
        File file = new File(spokePath(path));
        File[] files = file.listFiles((FilenameFilter) null);
        if(index < 0) index = files.length - 1;  // mystery meaning for negative index
        if(files.length < index) {
            return null;
        }else{
            return keyPart(files[index].getAbsolutePath());
        }
    }


//    public byte[] readNextItem(String path){
//        Path p = Paths.get(spokePath(path));
//        Path parent = p.getParent();
//        File parentFolder = new File(parent.toString());
//        String file = p.getFileName().toString();
//        // TODO bc 11/17/14: finish implementation
//        File[] files = parentFolder.listFiles((FileFilter) FileFileFilter.FILE);
//        // find file in listing
//        String nextPath = "";
//        return read(nextPath);
//    }

    public Collection<String> keysInBucket(String path){
        Collection<File> files = FileUtils.listFiles(new File(spokePath(path)), null, true);
        // return array of strings with just keys
        Vector keys = new Vector();

        for(File file : files){
            keys.add(keyPart(file.getPath()));
        }
        return keys;
    }

    public byte[] readKeysInBucket(String path){
        Collection<String> keys = keysInBucket(path);
        StringBuilder tmp = new StringBuilder();
        for(String key : keys){
            tmp.append(key);
            tmp.append(",");
        }
        return tmp.toString().getBytes();
    }

    public boolean delete(String path) throws Exception {
        FileUtils.deleteDirectory(new File(storagePath + path));
        return true;
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

    */
}
