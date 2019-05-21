package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.DocumentationDao;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.function.Function;

@Slf4j
public class FileDocumentationDao implements DocumentationDao {

    private final static String FILENAME = "documentation";
    private final FileUtil fileUtil;

    @Inject
    public FileDocumentationDao(FileUtil fileUtil) {
        this.fileUtil = fileUtil;
    }

    @Override
    public String get(String channel) {
        log.trace("getting documentation for channel {}", channel);
        String path = getDocumentationPath(channel);
        String content = this.fileUtil.read(path, FILENAME, Function.identity());
        return (content != null) ? content : "";
    }

    @Override
    public boolean upsert(String channel, byte[] bytes) {
        String path = getDocumentationPath(channel);
        log.trace("saving {} bytes to {}", path + FILENAME);
        return fileUtil.write(bytes, FILENAME, path);
    }

    @Override
    public boolean delete(String channel) {
        log.trace("deleting documentation for {}", channel);
        String path = getDocumentationPath(channel);
        return fileUtil.delete(path + FILENAME);
    }

    private String getDocumentationPath(String channel) {
        return fileUtil.getStoragePath() + "content/" + channel + "/";
    }
}
