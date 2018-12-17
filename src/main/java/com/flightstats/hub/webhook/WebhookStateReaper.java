package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubServices;
import com.flightstats.hub.cluster.LastContentPath;
import com.flightstats.hub.dao.Dao;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.AbstractScheduledService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static com.flightstats.hub.app.HubServices.register;
import static com.flightstats.hub.webhook.WebhookLeader.WEBHOOK_LAST_COMPLETED;
import static java.util.stream.Collectors.toList;

@Singleton
public class WebhookStateReaper {
    private final static Logger logger = LoggerFactory.getLogger(WebhookStateReaper.class);

    private final LastContentPath lastContentPath;
    private final WebhookContentPathSet webhookInProcess;
    private final WebhookError webhookError;
    private final Dao<Webhook> webhookDao;

    @Inject
    WebhookStateReaper(LastContentPath lastContentPath,
                       WebhookContentPathSet webhookInProcess,
                       WebhookError webhookError,
                       @Named("Webhook") Dao<Webhook> webhookDao) {
        this.lastContentPath = lastContentPath;
        this.webhookInProcess = webhookInProcess;
        this.webhookError = webhookError;
        this.webhookDao = webhookDao;

        register(new WebhookStateReaperService(), HubServices.TYPE.AFTER_HEALTHY_START);
    }

    void delete(String webhook) {
        logger.info("deleting " + webhook);
        webhookInProcess.delete(webhook);
        lastContentPath.delete(webhook, WEBHOOK_LAST_COMPLETED);
        webhookError.delete(webhook);
        logger.info("deleted " + webhook);
    }

    @VisibleForTesting
    void reapStateForDeletedWebhooks() {
        List<String> daoWebhooks = new HashSet<>(webhookDao.getAll(false)).stream()
                .map(Webhook::getName)
                .collect(toList());

        Stream.of(webhookInProcess.getWebhooks(), webhookError.getWebhooks(), lastContentPath.getNames(WEBHOOK_LAST_COMPLETED))
                .flatMap(Set::stream)
                .distinct()
                .filter(webhook -> !daoWebhooks.contains(webhook))
                .forEach(this::delete);
    }

    private class WebhookStateReaperService extends AbstractScheduledService {
        @Override
        protected void runOneIteration() {
            reapStateForDeletedWebhooks();
        }

        @Override
        protected Scheduler scheduler() {
            return Scheduler.newFixedRateSchedule(0, 1, TimeUnit.DAYS);
        }
    }
}
