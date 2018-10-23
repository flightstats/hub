package com.flightstats.hub.spoke;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import javax.ws.rs.NotFoundException;
import java.io.*;
import java.util.Arrays;
import java.util.Collection;

/**
 * Direct interactions with the file system
 */
public class FileSpokeStore {

    private final static Logger logger = LoggerFactory.getLogger(FileSpokeStore.class);
    private final String spokePath;
    private final int spokeTtlMinutes;

    public FileSpokeStore(String spokePath, int spokeTtlMinutes) {
        this.spokePath = StringUtils.appendIfMissing(spokePath, "/");
        this.spokeTtlMinutes = spokeTtlMinutes;
        logger.info("starting with storage path " + this.spokePath);
        if (!insert("hub-startup/" + new ContentKey().toUrl(), ("" + System.currentTimeMillis()).getBytes())) {
            throw new RuntimeException("unable to create startup file");
        }
        File file = spokeFilePathPart("hub-startup/" + new ContentKey().toUrl());
        if (file.canExecute()) {
            logger.warn("**** Spoke file permissions may allow incomplete reads ****");
        }
    }

    public boolean insert(String path, byte[] payload) {
        return insert(path, new ByteArrayInputStream(payload));
    }

    public boolean insert(String path, InputStream input) {
        File file = spokeFilePathPart(path);
        logger.trace("insert {} {} {}", file, file.getParentFile().mkdirs(), file.canExecute());
        try (FileOutputStream output = new FileOutputStream(file)) {
            long copy = ByteStreams.copy(input, output);
            boolean setExecutable = file.setExecutable(true);
            logger.trace("copied {} {} {}", file, copy, setExecutable);
            return true;
        } catch (IOException e) {
            logger.info("unable to write to " + path, e);
            return false;
        }
    }

    public byte[] read(String path) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        read(path, baos);
        return baos.toByteArray();
    }

    public void read(String path, OutputStream output) {
        File file = spokeFilePathPart(path);
        logger.trace("reading {}", file);
        if (!file.exists()) {
            throw new NotFoundException("not found " + path);
        }
        if (!file.canExecute()) {
            logger.warn("incomplete file {}", path);
            throw new NotFoundException("incomplete file " + path);
        }
        try (FileInputStream input = new FileInputStream(file)) {
            ByteStreams.copy(input, output);
        } catch (FileNotFoundException e) {
            logger.debug("file not found {}", path);
        } catch (IOException e) {
            logger.info("unable to read from " + path, e);
        }
    }

    public String readKeysInBucket(String path) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keysInBucket(path, baos);
        return baos.toString();
    }

    void readKeysInBucket(String path, OutputStream output) {
        keysInBucket(path, output);
    }

    public boolean delete(String path) throws Exception {
        FileUtils.deleteDirectory(new File(spokePath + path));
        return true;
    }

    public boolean deleteFile(String path) throws Exception {
        return FileUtils.deleteQuietly(spokeFilePathPart(path));
    }

    // given a url containing a key, return the file format
    // example: "test_0_4274725520517677/2014/11/18/00/57/24/015/NV2cl5"
    @VisibleForTesting
    File spokeFilePathPart(String urlPathPart) {
        String[] split = StringUtils.split(urlPathPart, "/");
        if (split.length >= 7 && split.length <= 8)
            return new File(spokePath + split[0] + "/" + split[1] + "/" + split[2] + "/" + split[3] + "/" + split[4]
                    + "/" + split[5]);
        if (split.length < 7)
            return new File(spokePath + urlPathPart);
        return new File(spokePath + split[0] + "/" + split[1] + "/" + split[2] + "/" + split[3] + "/" + split[4]
                + "/" + split[5] + "/" + split[6] + split[7] + split[8]);
    }


    //Given a File, return a key part (full key, or time path part)
    String spokeKeyFromPath(String path) {
        if (path.contains(spokePath))
            path = path.substring(spokePath.length());

        // file or directory?
        int i = path.lastIndexOf("/");
        String suffix = path.substring(i + 1);
        if (suffix.length() > 4) {
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

    private void keysInBucket(String key, OutputStream output) {
        String path = spokeFilePathPart(key).getAbsolutePath();
        logger.trace("path {}", path);
        String resolution = SpokePathUtil.smallestTimeResolution(key);
        File directory = new File(path);

        if (!directory.exists()) {
            return;
        }
        try {
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
                String keyFromPath = spokeKeyFromPath(aFile.getAbsolutePath());
                writeKey(output, keyFromPath);
            }
        } catch (Exception e) {
            logger.info("error with " + path, e);
        }
    }

    private void writeKey(OutputStream output, String keyFromPath) throws IOException {
        output.write(keyFromPath.getBytes());
        output.write(",".getBytes());
    }

    @VisibleForTesting
    Collection<String> keysInBucket(String key) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        keysInBucket(key, baos);
        String[] split = baos.toString().split(",");
        return Arrays.asList(split);
    }

    public String getLatest(String channel, String limitPath) {
        logger.trace("latest {} {}", channel, limitPath);
        ContentKey limitKey = ContentKey.fromUrl(limitPath).get();
        return getLatest(channel, limitPath, limitKey.getTime());
    }

    private String getLatest(String channel, String limitPath, DateTime hourToSearch) {
        logger.trace("latest {} {} {}", channel, limitPath, hourToSearch);
        String hoursPath = TimeUtil.hours(hourToSearch);
        String fullHoursPath = spokePath + channel + "/" + hoursPath;
        String[] minutes = new File(fullHoursPath).list();
        if (minutes == null) {
            minutes = new String[0];
        }
        Arrays.sort(minutes);
        logger.trace("looking at {} {}", fullHoursPath, minutes);
        for (int i = minutes.length - 1; i >= 0; i--) {
            String minute = minutes[i];
            String[] fileNames = new File(fullHoursPath + "/" + minute).list();
            Arrays.sort(fileNames);
            for (int j = fileNames.length - 1; j >= 0; j--) {
                String spokeKeyFromPath = spokeKeyFromPath(hoursPath + "/" + minute + "/" + fileNames[j]);
                logger.trace("looking at file {} ", spokeKeyFromPath);
                if (spokeKeyFromPath.compareTo(limitPath) < 0) {
                    return channel + "/" + spokeKeyFromPath;
                }
            }
        }
        DateTime ttlTime = TimeUtil.now().minusMinutes(spokeTtlMinutes);
        DateTime previous = hourToSearch.minusHours(1).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999);
        if (previous.isBefore(ttlTime)) {
            logger.debug("no latest found for {} {} ", channel, limitPath);
            return null;
        }
        return getLatest(channel, limitPath, previous);
    }

    /**
     * This may return more than the request count, as this does not do any sorting.
     */
    public void getNext(String channel, String startKey, int count, OutputStream output) throws IOException {
        DateTime now = TimeUtil.now();
        String channelPath = spokePath + channel + "/";
        logger.trace("next {} {} {}", channel, startKey, now);
        ContentKey start = ContentKey.fromUrl(startKey).get();
        int found = 0;
        MinutePath minutePath = new MinutePath(start.getTime());
        boolean firstMinute = true;
        do {
            //todo gfm - while this fast for short time ranges, it is quite slow over years
            String minuteUrl = minutePath.toUrl();
            String minute = channelPath + minuteUrl;
            logger.trace("minute {}", minute);
            String[] items = new File(minute).list();
            if (items != null) {
                for (String item : items) {
                    String keyFromPath = spokeKeyFromPath(minuteUrl + "/" + item);
                    if (firstMinute) {
                        ContentKey key = ContentKey.fromUrl(keyFromPath).get();
                        if (key.compareTo(start) > 0) {
                            found++;
                            writeKey(output, channel + "/" + keyFromPath);
                        }
                    } else {
                        found++;
                        writeKey(output, channel + "/" + keyFromPath);
                    }
                }
            }
            minutePath = new MinutePath(minutePath.getTime().plusMinutes(1));
            firstMinute = false;
        } while (found < count && minutePath.getTime().isBefore(now));
    }

    void enforceTtl(String channel, DateTime dateTime) {
        String limitPath = TimeUtil.minutes(dateTime);
        logger.debug("enforceTtl {} {}", channel, limitPath);
        String[] split = StringUtils.split(limitPath, "/");
        split = new String[]{split[0], split[1], split[2], split[3], split[4]};
        recurseDelete(channel, split, 0, channel);
    }

    private void recurseDelete(String path, String[] limitPath, int count, String channel) {
        logger.trace("recurse delete {} {}", path, count);
        String pathname = spokePath + path;
        String[] items = new File(pathname).list();
        if (items == null) {
            logger.trace("path not found {}", pathname);
            return;
        }
        String limitCompare = channel + "/";
        for (int i = 0; i <= count; i++) {
            limitCompare += limitPath[i] + "/";
        }
        for (String item : items) {
            logger.info("looking at {} {}", item, limitCompare);
            String current = path + "/" + item + "/";
            if (current.compareTo(limitCompare) <= 0) {
                if (count < 4) {
                    recurseDelete(path + "/" + item, limitPath, count + 1, channel);
                } else {
                    logger.info("deleting {}", spokePath + "/" + current);
                    FileUtils.deleteQuietly(new File(spokePath + "/" + current));
                }
            }
        }
    }

}
