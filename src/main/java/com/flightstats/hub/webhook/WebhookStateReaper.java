package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.LastContentPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

import static com.flightstats.hub.webhook.WebhookLeader.WEBHOOK_LAST_COMPLETED;

@Singleton
class WebhookStateReaper {
    private final static Logger logger = LoggerFactory.getLogger(WebhookStateReaper.class);

    private final LastContentPath lastContentPath;
    private final WebhookContentPathSet webhookInProcess;
    private final WebhookErrorService webhookErrorService;

    @Inject
    WebhookStateReaper(LastContentPath lastContentPath,
                       WebhookContentPathSet webhookInProcess,
                       WebhookErrorService webhookErrorService) {
        this.lastContentPath = lastContentPath;
        this.webhookInProcess = webhookInProcess;
        this.webhookErrorService = webhookErrorService;
    }

    void delete(String webhook) {
        logger.info("deleting " + webhook);
        webhookInProcess.delete(webhook);
        lastContentPath.delete(webhook, WEBHOOK_LAST_COMPLETED);
        webhookErrorService.delete(webhook);
        logger.info("deleted " + webhook);
    }
}
