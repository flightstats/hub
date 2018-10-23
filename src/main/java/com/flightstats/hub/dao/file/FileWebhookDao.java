package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.io.File;
import java.util.Collection;

public class FileWebhookDao implements Dao<Webhook> {

    private final static Logger logger = LoggerFactory.getLogger(FileWebhookDao.class);
    private final WebhookService webhookService;
    private final Gson gson;
    private final String groupPath;

    @Inject
    public FileWebhookDao(WebhookService webhookService, Gson gson) {
        this.webhookService = webhookService;
        this.gson = gson;
        groupPath = FileUtil.getStoragePath() + "group/";
        logger.info("using channel path {}", groupPath);
    }

    @Override
    public void upsert(Webhook webhook) {
        FileUtil.write(gson.toJson(webhook), webhook.getName(), groupPath);
    }

    @Override
    public Webhook get(String name) {
        return FileUtil.read(groupPath, name, webhookService::fromJson);
    }

    @Override
    public Collection<Webhook> getAll(boolean useCache) {
        return FileUtil.getIterable(groupPath, webhookService::fromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(groupPath + name));
    }

}
