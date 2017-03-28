package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.DocumentationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class FileDocumentationDao implements DocumentationDao {

    private final static Logger logger = LoggerFactory.getLogger(FileDocumentationDao.class);
    private final static String FILENAME = "documentation";

    @Override
    public String get(String channel) {
        logger.trace("getting documentation for channel {}", channel);
        String path = getDocumentationPath(channel);
        String content = FileUtil.read(path, FILENAME, Function.identity());
        return (content != null) ? content : "";
    }

    @Override
    public boolean upsert(String channel, byte[] bytes) {
        String path = getDocumentationPath(channel);
        logger.trace("saving {} bytes to {}", path + FILENAME);
        return FileUtil.write(bytes, FILENAME, path);
    }

    @Override
    public boolean delete(String channel) {
        logger.trace("deleting documentation for {}", channel);
        String path = getDocumentationPath(channel);
        return FileUtil.delete(path + FILENAME);
    }

    private String getDocumentationPath(String channel) {
        return FileUtil.getStoragePath() + "content/" + channel + "/";
    }
}
