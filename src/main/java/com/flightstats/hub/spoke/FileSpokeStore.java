package com.flightstats.hub.spoke;

import static com.flightstats.hub.constant.ContentConstant.SPOKE_TMP_SUFFIX;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

import com.amazonaws.util.CollectionUtils;
import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.MinutePath;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.TimeUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.io.ByteStreams;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

/**
 * Direct interactions with the file system
 */
@Slf4j
public class FileSpokeStore {
    private static final String SECOND_RESOLUTION = "second";
    private final String spokePath;
    private final int spokeTtlMinutes;
    private final Set<String> filesArtificiallyLocked = ConcurrentHashMap.newKeySet();

    public FileSpokeStore(String spokePath, int spokeTtlMinutes) {
        this.spokePath = StringUtils.appendIfMissing(spokePath, HubUtils.FILE_SYSTEM_SEPARATOR);
        this.spokeTtlMinutes = spokeTtlMinutes;
        log.info("starting with storage path " + this.spokePath);
        if (!insert("hub-startup/" + new ContentKey().toUrl(), ("" + System.currentTimeMillis()).getBytes())) {
            throw new RuntimeException("unable to create startup file");
        }
    }

    public boolean insert(String path, byte[] payload) {
        return insert(path, new ByteArrayInputStream(payload));
    }

    @SneakyThrows
    public boolean insert(String path, InputStream input) {
        File file = spokeFilePathPart(path);
        File tmpFile = spokeFilePathPart(path + SPOKE_TMP_SUFFIX);
        Stream.of(file, tmpFile).forEach(f -> f.getParentFile().mkdirs());
        log.trace("insert {}", file);
        filesArtificiallyLocked.add(path);
        try {
            try (FileOutputStream output = new FileOutputStream(tmpFile)) {
                long copy = ByteStreams.copy(input, output);
                log.trace("copied {} {}", file, copy);
            } catch (IOException e) {
                log.error("Error writing to spoke path (tmp file phase) {}", tmpFile.getPath(), e);
                return false;
            }
            Files.move(tmpFile.toPath(), file.toPath(), ATOMIC_MOVE);
            return true;
        } catch (IOException e) {
            log.error("Error moving file to spoke channel destination after write");
            return false;
        } finally {
            filesArtificiallyLocked.remove(path);
        }
    }

    @SneakyThrows
    public byte[] read(String path) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        read(path, byteArrayOutputStream);
        return byteArrayOutputStream.toByteArray();
    }

    @SneakyThrows
    public void read(String path, OutputStream output) {
        File file = spokeFilePathPart(path);
        if (!file.exists()) {
            throw new NotFoundException("not found " + path);
        }
        if (!file.canRead()) {
            log.error("Permission denied reading File: {}", path);
            throw new AccessDeniedException("Permission denied reading File" + path);
        }
        if (filesArtificiallyLocked.contains(path)) {
            log.error("File is locked by write operator");
            throw new OverlappingFileLockException();
        }
        try (FileInputStream input = new FileInputStream(file)) {
            log.trace("reading {}", file);
            ByteStreams.copy(input, output);
        } catch (FileNotFoundException e) {
            log.error("file not found {}", path);
        } catch (IOException e) {
            log.error("unable to read from " + path, e);
        }
    }

    public String readKeysInBucket(String path) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        readKeysInBucket(path, baos);
        return baos.toString();
    }

    @VisibleForTesting
    Collection<String> getKeysInBucketArray(String key) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        readKeysInBucket(key, baos);
        String[] split = baos.toString().split(",");
        return Arrays.asList(split);
    }

    public boolean delete(String path) throws Exception {
        FileUtils.deleteDirectory(new File(spokePath + path));
        return true;
    }

    public boolean deleteFile(String path) throws Exception {
        return FileUtils.deleteQuietly(spokeFilePathPart(path));
    }

    // given an url containing a key, return the file format
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
            path = path.substring(path.lastIndexOf(spokePath)+spokePath.length());

        log.info("spoke key path {}",path);

        // file or directory?
        int i = path.lastIndexOf(HubUtils.FILE_SYSTEM_SEPARATOR);
        String suffix = path.substring(i + 1);
        if (suffix.length() > 4) {
            // presence of second proves file aims at a full payload path
            String folderPath = path.substring(0, i);
            String seconds = suffix.substring(0, 2);
            String milliseconds = suffix.substring(2, 5);
            String hash = suffix.substring(5);
            return HubUtils.getNormalizedFilePath(folderPath ,seconds , milliseconds , hash);
        }
        // file is a directory
        return HubUtils.getNormalizedFilePath(path);
    }

    public void readKeysInBucket(String key, OutputStream output) {
        String path = spokeFilePathPart(key).getAbsolutePath();
        log.trace("path {}", path);
        String resolution = SpokePathUtil.smallestTimeResolution(key);
        File directory = new File(path);

        if (!directory.exists()) {
            return;
        }
        try {
            if (SECOND_RESOLUTION.equals(resolution)) {
                // filter all files in the minute folder that start with seconds
                FileFilter fileFilter = new WildcardFileFilter(SpokePathUtil.second(key) + "*");
                List<File> files = Arrays.asList(Optional.ofNullable(directory.listFiles(fileFilter)).orElse(new File[]{}));
                writeFilesToOutput(files, output);
            } else {
                writeFilesToOutput(FileUtils.listFiles(new File(path), null, true), output);
            }
        } catch (Exception e) {
            log.error("error with " + path, e);
        }
    }

    private void writeFilesToOutput(Collection<File> files, OutputStream outputStream) {
        files.stream()
                .filter(f -> !isTempFile(f))
                .forEach(file -> {
                    try {
                        log.trace("file path: {}", file.getPath());
                        writeKey(outputStream, spokeKeyFromPath(file.getAbsolutePath()));
                    } catch (IOException e) {
                        log.error("Error writing file to output ", e);
                    }
                });
    }

    private void writeKey(OutputStream output, String keyFromPath) throws IOException {
        output.write(keyFromPath.getBytes());
        output.write(",".getBytes());
    }

    public String getLatest(String channel, String limitPath){
        log.trace("latest {} {}", channel, limitPath);
        ContentKey limitKey = ContentKey.fromUrl(limitPath)
                .orElseThrow(() -> new RuntimeException("Could not parse ContentKey: " + limitPath));
        return getLatest(channel, limitPath, limitKey.getTime());
    }
    private List<String> listDirsOnPath(Path normalizedPath){
        return listDirsOnPathByFilter(normalizedPath, (fileName) -> true);
    }

    private List<String> listDirsOnPathByFilter(Path normalizedPath, Function<String, Boolean> fileNameFilter){
        List<String> listDirs = null;
        try(Stream<Path> streamPaths = Files.list(normalizedPath))  {
            listDirs =
                    streamPaths.map(path -> path.getFileName().toString()).filter(fileNameFilter::apply)
                            .sorted().collect(
                                    Collectors.toList());
        } catch (IOException e) {
            log.warn("Warning - No data found at path - {}.", normalizedPath);
        } finally {
            if(CollectionUtils.isNullOrEmpty(listDirs)){
                listDirs = Collections.emptyList();
            }
        }
        log.debug("Directories at Path {} - {}.", normalizedPath, listDirs);
        return listDirs;
    }

    private String getLatest(String channel, String limitPath, DateTime hourToSearch){
        log.trace("Latest channel - {}, limitpath - {}, hourToSearch - {}", channel, limitPath, hourToSearch);
        String hoursPath = TimeUtil.hours(hourToSearch);
        Path normalizeFullHoursPath = Paths.get(spokePath + channel, hoursPath).normalize();
        List<String> minutes = listDirsOnPath(normalizeFullHoursPath);
        if(!CollectionUtils.isNullOrEmpty(minutes)) {
            Collections.reverse(minutes);
            log.trace("Looking at path - {}, sub-dirs - {}", normalizeFullHoursPath.getFileName(), minutes);
            for (String minute : minutes) {
                Path normalizedHoursMins = Paths.get(normalizeFullHoursPath.toString(), minute)
                        .normalize();
                List<String> fileNames = listDirsOnPathByFilter(normalizedHoursMins, (fileName) -> !isTempFile(fileName));
                if(!CollectionUtils.isNullOrEmpty(fileNames)) {
                    Collections.reverse(fileNames);
                    for (String fileName : fileNames) {
                        String spokeKeyFromPath = spokeKeyFromPath(HubUtils.getNormalizedFilePath(hoursPath,minute,fileName));
                        log.trace("Looking at file {} .", spokeKeyFromPath);
                        if (HubUtils.getNormalizedFilePath(spokeKeyFromPath).compareTo(HubUtils.getNormalizedFilePath(limitPath)) < 0) {
                            return HubUtils.getNormalizedFilePath(channel,spokeKeyFromPath);
                        }
                    }
                }
            }
        }
        DateTime ttlTime = TimeUtil.now().minusMinutes(spokeTtlMinutes);
        DateTime previous = hourToSearch.minusHours(1).withMinuteOfHour(59).withSecondOfMinute(59).withMillisOfSecond(999);
        if (previous.isBefore(ttlTime)) {
            log.debug("No latest found for {} at path {} ", channel, limitPath);
            return null;
        }
        return getLatest(channel, limitPath, previous);
    }

    private boolean isTempFile(File file) {
        return isTempFile(file.getName());
    }

    private boolean isTempFile(String pathName) {
        return pathName.endsWith(SPOKE_TMP_SUFFIX);
    }


    /**
     * This may return more than the request count, as this does not do any sorting.
     */
    public void getNext(String channel, String startKey, int count, OutputStream output) throws IOException {
        DateTime now = TimeUtil.now();
        String channelPath = spokePath + channel + HubUtils.FILE_SYSTEM_SEPARATOR;
        log.trace("next {} {} {}", channel, startKey, now);
        ContentKey start = ContentKey
                .fromUrl(startKey)
                .orElseThrow(() -> new RuntimeException("Could not parse ContentKey: " + startKey));
        AtomicInteger found = new AtomicInteger();
        AtomicBoolean firstMinute = new AtomicBoolean(true);
        MinutePath minutePath = new MinutePath(start.getTime());
        do {
            //todo gfm - while this fast for short time ranges, it is quite slow over years
            String minuteUrl = minutePath.toUrl();
            String minute = channelPath + minuteUrl;
            log.trace("minute {}", minute);
            Arrays.stream(Optional.ofNullable(new File(minute).list())
                            .orElse(new String[]{}))
                    .filter(f -> !isTempFile(f))
                    .forEach(item -> {
                        String keyFromPath = spokeKeyFromPath(HubUtils.getNormalizedFilePath(minuteUrl ,item));
                        String fullKey = HubUtils.getNormalizedFilePath(channel,keyFromPath);
                        if (firstMinute.get()) {
                            ContentKey.fromUrl(keyFromPath).filter((key) -> key.compareTo(start) > 0)
                                    .ifPresent((key) -> writeNext(found, output, fullKey));
                        } else {
                            writeNext(found, output, fullKey);
                        }
                    });
            minutePath = new MinutePath(minutePath.getTime().plusMinutes(1));
            firstMinute.getAndSet(false);
        } while (found.get() < count && minutePath.getTime().isBefore(now));
    }

    private void writeNext(AtomicInteger found, OutputStream outputStream, String key) {
        try {
            found.getAndIncrement();
            writeKey(outputStream, key);
        } catch (IOException e) {
            log.error("Error writing read data to output stream ", e);
        }
    }

    void enforceTtl(String channel, DateTime dateTime) {
        String limitPath = TimeUtil.minutes(dateTime);
        log.debug("enforceTtl {} {}", channel, limitPath);
        String[] split = StringUtils.split(limitPath, "/");
        split = new String[]{split[0], split[1], split[2], split[3], split[4]};
        recurseDelete(channel, split, 0, channel);
    }

    private void recurseDelete(String path, String[] limitPath, int count, String channel) {
        log.trace("recurse delete {} {}", path, count);
        String pathname = spokePath + path;
        String[] items = new File(pathname).list();
        if (items == null) {
            log.trace("path not found {}", pathname);
            return;
        }
        StringBuilder limitCompareBuilder = new StringBuilder(channel + "/");
        for (int i = 0; i <= count; i++) {
            limitCompareBuilder.append(limitPath[i]).append("/");
        }
        String limitCompare = limitCompareBuilder.toString();
        for (String item : items) {
            log.debug("looking at {} {}", item, limitCompare);
            String current = path + "/" + item + "/";
            if (current.compareTo(limitCompare) <= 0) {
                if (count < 4) {
                    recurseDelete(path + "/" + item, limitPath, count + 1, channel);
                } else {
                    log.debug("deleting {}", spokePath + "/" + current);
                    FileUtils.deleteQuietly(new File(spokePath + "/" + current));
                }
            }
        }
    }
}