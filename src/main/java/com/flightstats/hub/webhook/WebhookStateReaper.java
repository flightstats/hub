package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.Dao;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.flightstats.hub.webhook.WebhookLeader.WEBHOOK_LAST_COMPLETED;

@Singleton
public class WebhookStateReaper {
    private final static Logger logger = LoggerFactory.getLogger(WebhookStateReaper.class);

    private final LastContentPath lastContentPath;
    private final WebhookContentPathSet webhookInProcess;
    private final WebhookError webhookError;

    @Inject
    WebhookStateReaper(LastContentPath lastContentPath,
                       WebhookContentPathSet webhookInProcess,
                       WebhookError webhookError) {
        this.lastContentPath = lastContentPath;
        this.webhookInProcess = webhookInProcess;
        this.webhookError = webhookError;
    }

    void delete(String webhook) {
        logger.info("deleting " + webhook);
        webhookInProcess.delete(webhook);
        lastContentPath.delete(webhook, WEBHOOK_LAST_COMPLETED);
        webhookError.delete(webhook);
        logger.info("deleted " + webhook);
    }
}
