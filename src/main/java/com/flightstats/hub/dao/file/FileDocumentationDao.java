package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.DocumentationDao;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Function;

public class FileDocumentationDao implements DocumentationDao {

    private final static Logger logger = LoggerFactory.getLogger(FileDocumentationDao.class);

    @Override
    public String get(String channel) {
        logger.info("get doc for {}", channel);
        String path = FileUtil.getStoragePath() + "content/" + channel + "/";
        String content = FileUtil.read(path, "documentation", Function.identity());
        return (content != null) ? content : "";
    }

    @Override
    public boolean upsert(String channel, byte[] bytes) {
        logger.info("upsert doc for {} {}", channel, new String(bytes));
        String path = FileUtil.getStoragePath() + "content/" + channel + "/";
        return FileUtil.write(bytes, "documentation", path);
    }
}
