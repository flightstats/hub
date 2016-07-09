package com.flightstats.hub.dao.nas;

import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookDao;
import com.google.common.base.Optional;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;

public class NasWebhookDao implements WebhookDao {

    private final static Logger logger = LoggerFactory.getLogger(NasWebhookDao.class);
    private final String groupPath;

    public NasWebhookDao() {
        groupPath = NasUtil.getStoragePath() + "group/";
        logger.info("using channel path {}", groupPath);
    }

    @Override
    public Webhook upsert(Webhook webhook) {
        NasUtil.writeJson(webhook.toJson(), webhook.getName(), groupPath);
        return webhook;
    }

    @Override
    public Optional<Webhook> get(String name) {
        return Optional.fromNullable(NasUtil.readJson(groupPath, name, Webhook::fromJson));
    }

    @Override
    public Collection<Webhook> getAll() {
        return NasUtil.getIterable(groupPath, Webhook::fromJson);
    }

    @Override
    public void delete(String name) {
        FileUtils.deleteQuietly(new File(groupPath + name));
    }

}
