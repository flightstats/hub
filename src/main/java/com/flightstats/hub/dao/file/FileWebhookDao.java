package com.flightstats.hub.dao.file;

import com.flightstats.hub.config.properties.SpokeProperties;
import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.webhook.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;

@Slf4j
public class FileWebhookDao implements Dao<Webhook> {

    private final SpokeFile spokeFile;
    private final String groupPath;

    @Inject
    public FileWebhookDao(SpokeFile spokeFile, SpokeProperties spokeProperties) {
        this.spokeFile = spokeFile;
        groupPath = spokeProperties.getStoragePath() + "group/";
        log.info("using channel path {}", groupPath);
    }

    @Override
    public void upsert(Webhook webhook) {
        spokeFile.write(webhook.toJson(), webhook.getName(), groupPath);
    }

    @Override
    public Webhook get(String name) {
        return spokeFile.read(groupPath, name, Webhook::fromJson);
    }

    @Override
    public Collection<Webhook> getAll(boolean useCache) {
        return spokeFile.getIterable(groupPath, Webhook::fromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(groupPath + name));
    }

}
