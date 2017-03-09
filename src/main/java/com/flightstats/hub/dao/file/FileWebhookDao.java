package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.webhook.Webhook;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

public class FileWebhookDao implements Dao<Webhook> {

    private final static Logger logger = LoggerFactory.getLogger(FileWebhookDao.class);
    private final String groupPath;

    public FileWebhookDao() {
        groupPath = FileUtil.getStoragePath() + "group/";
        logger.info("using channel path {}", groupPath);
    }

    @Override
    public void upsert(Webhook webhook) {
        FileUtil.write(webhook.toJson(), webhook.getName(), groupPath);
    }

    @Override
    public Webhook get(String name) {
        return FileUtil.read(groupPath, name, Webhook::fromJson);
    }

    @Override
    public Collection<Webhook> getAll(boolean useCache) {
        return FileUtil.getIterable(groupPath, Webhook::fromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(groupPath + name));
    }

}
