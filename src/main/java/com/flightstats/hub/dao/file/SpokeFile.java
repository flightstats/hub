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
            FileUtils.writeByteArrayToFile(new File(path + filename), bytes);
            return true;
        } catch (IOException e) {
            log.warn("unable to write file " + filename, e);
            return false;
        }
    }

    <T> T read(String path, String name, Function<String, T> function) {
        return read(new File(path + name), function);
    }

    <T> T read(String path, String name, BiFunction<String, ContentRetriever, T> function) {
        return read(new File(path + name), function);
    }

    private <T> T read(File file, Function<String, T> function) {
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

    private <T> T read(File file, BiFunction<String, ContentRetriever, T> function) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            return function.apply(new String(bytes), contentRetriever);
        } catch (FileNotFoundException e) {
            log.info("file not found {} {} ", file.getName(), e.getMessage());
        } catch (IOException e) {
            log.warn("unable to find for " + file.getName(), e);
        }
        return null;
    }

    <T> Collection<T> getIterable(String path, BiFunction<String, ContentRetriever, T> function) {
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

    <T> Collection<T> getIterable(String path, Function<String, T> function) {
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

    boolean delete(String filepath) {
        Path path = Paths.get(filepath);
        File file = path.toFile();
        return FileUtils.deleteQuietly(file);
    }
}
