package com.flightstats.hub.dao.file;

import com.flightstats.hub.app.HubProperties;
import lombok.Value;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

class FileUtil {

    private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);

    private static AtomicReference<String> storagePath;

    @Inject
    public void initialize(HubProperties hubProperties) {
        String path = StringUtils.appendIfMissing(hubProperties.getProperty("storage.path", "/file"), "/");
        storagePath.set(path);
    }

    static String getStoragePath() {
        return storagePath.get();
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
}
