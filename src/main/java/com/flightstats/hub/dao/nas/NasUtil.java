package com.flightstats.hub.dao.nas;

import com.flightstats.hub.app.HubProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

class NasUtil {

    private final static Logger logger = LoggerFactory.getLogger(NasUtil.class);

    static String getStoragePath() {
        return StringUtils.appendIfMissing(HubProperties.getProperty("storage.path", "/nas"), "/");
    }

    public static String getContentPath() {
        return NasUtil.getStoragePath() + "content/";
    }

    static void writeJson(String json, String name, String path) {
        try {
            byte[] bytes = json.getBytes();
            FileUtils.writeByteArrayToFile(new File(path + name), bytes);
        } catch (IOException e) {
            logger.warn("unable to save group for " + name, e);
        }
    }

    static <T> T readJson(String path, String name, Function<String, T> fromJson) {
        return read(new File(path + name), fromJson);
    }

    private static <T> T read(File file, Function<String, T> fromJson) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            return fromJson.apply(new String(bytes));
        } catch (FileNotFoundException e) {
            logger.info("file not found {} {} ", file.getName(), e.getMessage());
        } catch (IOException e) {
            logger.warn("unable to find for " + file.getName(), e);
        }
        return null;
    }

    static <T> Collection<T> getIterable(String path, Function<String, T> fromJson) {
        List<T> list = new ArrayList<>();
        File[] files = new File(path).listFiles();
        if (files == null) {
            return list;
        }
        for (File file : files) {
            list.add(read(file, fromJson));
        }
        return list;
    }

}
