package com.flightstats.hub.dao.nas;

import com.flightstats.hub.app.HubProperties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class NasUtil {

    private final static Logger logger = LoggerFactory.getLogger(NasUtil.class);

    public static String getStoragePath() {
        return StringUtils.appendIfMissing(HubProperties.getProperty("nas.storage.path", "/nas"), "/");
    }

    public static void writeJson(String json, String name, String path) {
        try {
            byte[] bytes = json.getBytes();
            FileUtils.writeByteArrayToFile(new File(path + name), bytes);
        } catch (IOException e) {
            logger.warn("unable to save group for " + name, e);
        }
    }

    public static <T> T readJson(String path, String name, Function<String, T> fromJson) {
        return read(new File(path + name), fromJson);
    }

    private static <T> T read(File file, Function<String, T> fromJson) {
        try {
            byte[] bytes = FileUtils.readFileToByteArray(file);
            return fromJson.apply(new String(bytes));
        } catch (IOException e) {
            logger.warn("unable to find for " + file.getName(), e);
        }
        return null;
    }

    public static <T> Iterable<T> getIterable(String path, Function<String, T> fromJson) {
        List<T> list = new ArrayList<>();
        File[] files = new File(path).listFiles();
        if (files == null) {
            return list;
        }
        for (int i = 0; i < files.length; i++) {
            list.add(read(files[i], fromJson));
        }
        return list;
    }

}
