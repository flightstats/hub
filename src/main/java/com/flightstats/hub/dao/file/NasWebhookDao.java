package com.flightstats.hub.dao.file;

import com.flightstats.hub.dao.Dao;
import com.flightstats.hub.webhook.Webhook;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

public class NasWebhookDao implements Dao<Webhook> {

    private final static Logger logger = LoggerFactory.getLogger(NasWebhookDao.class);
    private final String groupPath;

    public NasWebhookDao() {
        groupPath = NasUtil.getStoragePath() + "group/";
        logger.info("using channel path {}", groupPath);
    }

    @Override
    public void upsert(Webhook webhook) {
        NasUtil.writeJson(webhook.toJson(), webhook.getName(), groupPath);
    }

    @Override
    public Webhook get(String name) {
        return NasUtil.readJson(groupPath, name, Webhook::fromJson);
    }

    @Override
    public Collection<Webhook> getAll(boolean useCache) {
        return NasUtil.getIterable(groupPath, Webhook::fromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(groupPath + name));
    }

}
