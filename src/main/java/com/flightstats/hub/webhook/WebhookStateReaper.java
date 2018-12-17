package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.LastContentPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

import static com.flightstats.hub.webhook.WebhookLeader.WEBHOOK_LAST_COMPLETED;

public class WebhookStateReaper {
    private final static Logger logger = LoggerFactory.getLogger(WebhookStateReaper.class);

    private final LastContentPath lastContentPath;
    private final WebhookContentPathSet webhookInProcess;
    private final WebhookError webhookError;

    @Inject
    WebhookStateReaper(LastContentPath lastContentPath, WebhookContentPathSet webhookInProcess, WebhookError webhookError) {
        this.lastContentPath = lastContentPath;
        this.webhookInProcess = webhookInProcess;
        this.webhookError = webhookError;
    }

    void delete(String webhookName) {
        logger.info("deleting " + webhookName);
        webhookInProcess.delete(webhookName);
        lastContentPath.delete(webhookName, WEBHOOK_LAST_COMPLETED);
        webhookError.delete(webhookName);
        logger.info("deleted " + webhookName);
    }
}
