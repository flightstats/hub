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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public static boolean delete(String filepath) {
        Path path = Paths.get(filepath);
        File file = path.toFile();
        return FileUtils.deleteQuietly(file);
    }

    public static boolean directoryExists(String directoryPath) {
        Path path = Paths.get(directoryPath);
        return Files.exists(path);
    }

    public static void createDirectory(String directoryPath) throws IOException {
        try {
            Path path = Paths.get(directoryPath);
            Files.createDirectories(path);
        } catch (IOException e) {
            logger.warn("unable to create directory {}", directoryPath);
            throw e;
        }
    }

    public static void mergeDirectories(String from, String to) throws IOException {
        logger.info("merging {} into {}", from, to);

        Path source = Paths.get(from).toAbsolutePath();
        Path destination = Paths.get(to).toAbsolutePath();

        List<Path> paths = Files.walk(source).collect(Collectors.toList());
        for (int i = 0; i < paths.size(); ++i) {
            Path path = paths.get(i);

            if (!Files.exists(path)) {
                logger.debug("skipping, already moved {}", path);
                continue;
            }

            if (path.startsWith(destination)) {
                logger.debug("skipping, is destination {}", path);
                continue;
            }

            Path relativePath = source.relativize(path);
            Path proposedPath = new File(destination.toString(), relativePath.toString()).toPath();

            if (Files.exists(proposedPath)) {
                logger.debug("skipping, already exists {}", proposedPath);
                continue;
            }

            logger.info("moving {} to {}", path, proposedPath);
            Files.move(path, proposedPath);
        }
    }
}
