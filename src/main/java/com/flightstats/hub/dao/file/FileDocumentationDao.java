package com.flightstats.hub.dao.file;

import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.dao.DocumentationDao;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import java.util.function.Function;

@Slf4j
public class FileDocumentationDao implements DocumentationDao {

    private final static String FILENAME = "documentation";
    private final SpokeFile spokeFile;
    private final String storagePath;


    @Inject
    public FileDocumentationDao(SpokeFile spokeFile, SpokeProperties spokeProperties) {
        this.spokeFile = spokeFile;
        this.storagePath = spokeProperties.getStoragePath();
    }

    @Override
    public String get(String channel) {
        log.trace("getting documentation for channel {}", channel);
        String path = getDocumentationPath(channel);
        String content = this.spokeFile.read(path, FILENAME, Function.identity());
        return (content != null) ? content : "";
    }

    @Override
    public boolean upsert(String channel, byte[] bytes) {
        String path = getDocumentationPath(channel);
        log.trace("saving {} bytes to {}", path + FILENAME);
        return spokeFile.write(bytes, FILENAME, path);
    }

    @Override
    public boolean delete(String channel) {
        log.trace("deleting documentation for {}", channel);
        String path = getDocumentationPath(channel);
        return spokeFile.delete(path + FILENAME);
    }

    private String getDocumentationPath(String channel) {
        return storagePath + "content/" + channel + "/";
    }
}
