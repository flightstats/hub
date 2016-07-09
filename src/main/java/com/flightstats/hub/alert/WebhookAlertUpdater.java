package com.flightstats.hub.alert;

import com.flightstats.hub.model.ContentKey;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookStatus;
import org.joda.time.Minutes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.concurrent.Callable;

class WebhookAlertUpdater implements Callable<AlertStatus> {

    private final static Logger logger = LoggerFactory.getLogger(WebhookAlertUpdater.class);

    private final AlertConfig alertConfig;
    private final AlertStatus alertStatus;

    WebhookAlertUpdater(AlertConfig alertConfig, AlertStatus alertStatus) {
        this.alertConfig = alertConfig;
        if (alertStatus == null) {
            alertStatus = AlertStatus.builder()
                    .name(alertConfig.getName())
                    .alert(false)
                    .history(new LinkedList<>())
                    .build();
        }
        this.alertStatus = alertStatus;
        alertStatus.setType(alertConfig.getType().name());
    }

    @Override
    public AlertStatus call() throws Exception {
        alertStatus.getHistory().clear();
        WebhookStatus webhookStatus = WebhookState.getStatus(alertConfig);

        if (webhookStatus == null) {
            return alertStatus;
        }
        ContentKey channelLatest = webhookStatus.getChannelLatest();
        if (channelLatest == null) {
            return alertStatus;
        }
        addHistory(channelLatest, webhookStatus.getWebhook(), "channelLatest");
        ContentPath lastCompleted = webhookStatus.getLastCompleted();
        addHistory(lastCompleted, webhookStatus.getWebhook(), "lastCompletedCallback");
        Minutes minutes = Minutes.minutesBetween(lastCompleted.getTime(), channelLatest.getTime());
        logger.trace("alert {} latest {} completed {} minutes {}", alertConfig.getName(), channelLatest, lastCompleted, minutes);
        if (minutes.getMinutes() >= alertConfig.getTimeWindowMinutes()) {
            if (!alertStatus.isAlert()) {
                alertStatus.setAlert(true);
                AlertSender.sendAlert(alertConfig, alertStatus, minutes.getMinutes());
            }
        } else {
            alertStatus.setAlert(false);
        }
        return alertStatus;
    }

    private void addHistory(ContentPath contentPath, Webhook webhook, String name) {
        AlertStatusHistory history = AlertStatusHistory.builder()
                .href(webhook.getChannelUrl() + "/" + contentPath.toUrl())
                .name(name)
                .build();
        alertStatus.getHistory().add(history);
    }

}
