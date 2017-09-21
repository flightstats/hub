package com.flightstats.hub.dao.file;

import com.flightstats.hub.app.HubProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

public class FileUtil {

    private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    static String getStoragePath() {
        return StringUtils.appendIfMissing(HubProperties.getProperty("storage.path", "/file"), "/");
    }

    public static String getContentPath() {
        return FileUtil.getStoragePath() + "content/";
    }

    static boolean write(String content, String filename, String path) {
        return write(content.getBytes(), filename, path);
    }

    static boolean write(byte[] bytes, String filename, String path) {
        try {
            FileUtils.writeByteArrayToFile(new File(path + filename), bytes);
            return true;
        } catch (IOException e) {
            logger.warn("unable to write file " + filename, e);
            return false;
        }
    }

    static <T> T read(String path, String name, Function<String, T> function) {
        return read(new File(path + name), function);
    }

    private static <T> T read(File file, Function<String, T> function) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            return function.apply(new String(bytes));
        } catch (FileNotFoundException e) {
            logger.info("file not found {} {} ", file.getName(), e.getMessage());
        } catch (IOException e) {
            logger.warn("unable to find for " + file.getName(), e);
        }
        return null;
    }

    static <T> Collection<T> getIterable(String path, Function<String, T> function) {
        List<T> list = new ArrayList<>();
        File[] files = new File(path).listFiles();
        if (files == null) {
            return list;
        }
        for (File file : files) {
            list.add(read(file, function));
        }
        return list;
    }

    static boolean delete(String filepath) {
        Path path = Paths.get(filepath);
        File file = path.toFile();
        return FileUtils.deleteQuietly(file);
    }

    public static boolean directoryExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        return Files.exists(path);
    }

    public static void move(String from, String to) throws IOException {
        try {
            Path source = Paths.get(from);
            Path destination = Paths.get(to);
            if (source.equals(destination)) {
                logger.info("ignoring move request since the source and destination are the same");
            }
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            logger.warn("unable to move {} to {}", from, to);
            throw e;
        }
    }

    public static void createDirectory(String directoryPath) throws IOException {
        try {
            Path path = Paths.get(directoryPath);
            Files.createDirectory(path);
        } catch (IOException e) {
            logger.warn("unable to create directory {}", directoryPath);
            throw e;
        }
    }
}
