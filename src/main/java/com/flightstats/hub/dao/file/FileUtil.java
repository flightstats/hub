package com.flightstats.hub.dao.file;

import com.flightstats.hub.config.PropertiesLoader;
import com.flightstats.hub.config.SpokeProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

@Slf4j
class FileUtil {

    private static final SpokeProperties spokeProperties = new SpokeProperties(PropertiesLoader.getInstance());

    static String getStoragePath() {
        return StringUtils.appendIfMissing(spokeProperties.getStoragePath(), "/");
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
            log.warn("unable to write file " + filename, e);
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
            log.info("file not found {} {} ", file.getName(), e.getMessage());
        } catch (IOException e) {
            log.warn("unable to find for " + file.getName(), e);
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
}
