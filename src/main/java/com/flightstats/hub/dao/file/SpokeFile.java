package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.aws.ContentRetriever;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Slf4j
class SpokeFile {

    private final ContentRetriever contentRetriever;

    @Inject
    public SpokeFile(ContentRetriever contentRetriever) {
        this.contentRetriever = contentRetriever;
    }

    void write(String content, String filename, String path) {
        write(content.getBytes(), filename, path);
    }

    boolean write(byte[] bytes, String filename, String path) {
        try {
            File file = new File(path, filename);
            FileUtils.writeByteArrayToFile(file, bytes);
            return true;
        } catch (IOException e) {
            log.warn("Unable to write file {} at path {}", filename, path, e);
            return false;
        }
    }

    <T> T read(String path, String name, Function<String, T> function) {
        return read(new File(path, name), function);
    }

    <T> T read(String path, String name, BiFunction<String, ContentRetriever, T> function) {
        return read(new File(path, name), function);
    }

    private <T> T read(File file, Function<String, T> function) {
        if (!isSafePath(file)) {
            log.warn("Potential path traversal attempt: {}", file.getPath());
            return null;
        }

        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            return function.apply(new String(bytes));
        } catch (FileNotFoundException e) {
            log.warn("File not found: {} - {}", file.getPath(), e.getMessage());
        } catch (IOException e) {
            log.warn("Unable to read file: {} - {}", file.getPath(), e.getMessage());
        }
        return null;
    }

    private <T> T read(File file, BiFunction<String, ContentRetriever, T> function) {
        if (!isSafePath(file)) {
            log.warn("Potential path traversal attempt: {}", file.getPath());
            return null;
        }

        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            return function.apply(new String(bytes), contentRetriever);
        } catch (FileNotFoundException e) {
            log.info("File not found: {} - {}", file.getPath(), e.getMessage());
        } catch (IOException e) {
            log.warn("Unable to read file: {} - {}", file.getPath(), e.getMessage());
        }
        return null;
    }

    <T> Collection<T> getIterable(String path, BiFunction<String, ContentRetriever, T> function) {
        List<T> list = new ArrayList<>();
        File[] files = new File(path).listFiles();
        if (files == null) {
            log.warn("No files found at path {}", path);
            return list;
        }
        for (File file : files) {
            T item = read(file, function);
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    <T> Collection<T> getIterable(String path, Function<String, T> function) {
        List<T> list = new ArrayList<>();
        File[] files = new File(path).listFiles();
        if (files == null) {
            log.warn("No files found at path {}", path);
            return list;
        }
        for (File file : files) {
            T item = read(file, function);
            if (item != null) {
                list.add(item);
            }
        }
        return list;
    }

    boolean delete(String filepath) {
        Path path = Paths.get(filepath);
        File file = path.toFile();
        return FileUtils.deleteQuietly(file);
    }

    private boolean isSafePath(File file) {
        try {
            String canonicalPath = file.getCanonicalPath();
            String canonicalBasePath = new File(".").getCanonicalPath();
            return canonicalPath.startsWith(canonicalBasePath);
        } catch (IOException e) {
            log.warn("Failed to resolve canonical path for {}", file.getPath(), e);
            return false;
        }
    }
}
