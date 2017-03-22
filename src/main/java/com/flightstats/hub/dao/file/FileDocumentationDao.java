package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.DocumentationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class FileDocumentationDao implements DocumentationDao {

    private final static Logger logger = LoggerFactory.getLogger(FileDocumentationDao.class);

    @Override
    public String get(String channel) {
        logger.trace("get doc for {}", channel);
        String path = getDocumentationPath(channel);
        String content = FileUtil.read(path, "documentation", Function.identity());
        return (content != null) ? content : "";
    }

    @Override
    public boolean upsert(String channel, byte[] bytes) {
        logger.trace("upsert doc for {}", channel);
        String path = getDocumentationPath(channel);
        return FileUtil.write(bytes, "documentation", path);
    }

    @Override
    public boolean delete(String channel) {
        logger.trace("delete doc for {}", channel);
        String path = getDocumentationPath(channel);
        return FileUtil.delete(path + "documentation");
    }

    private String getDocumentationPath(String channel) {
        return FileUtil.getStoragePath() + "content/" + channel + "/";
    }
}
