package com.flightstats.hub.events;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;

import javax.inject.Inject;

class EventWebhook {

    private final String random = StringUtils.randomAlphaNumeric(6);
    private final ContentOutput contentOutput;
    private final WebhookService webhookService;
    private final HubProperties hubProperties;

    @Inject
    EventWebhook(ContentOutput contentOutput, WebhookService webhookService, HubProperties hubProperties) {
        this.contentOutput = contentOutput;
        this.webhookService = webhookService;
        this.hubProperties = hubProperties;
    }

    public void start() {
        Webhook.WebhookBuilder builder = Webhook.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(getChannelUrl())
                .heartbeat(true)
                .startingKey(contentOutput.getContentKey())
                .batch(Webhook.SECOND);
        Webhook webhook = builder.build();
        webhookService.upsert(webhook);
    }

    ContentOutput getContentOutput() {
        return contentOutput;
    }

    private String getChannelUrl() {
        return hubProperties.getAppUrl() + "channel/" + contentOutput.getChannel();
    }

    private String getCallbackUrl() {
        return HubHost.getLocalHttpIpUri() + "/internal/events/" + getGroupName();
    }

    public String getGroupName() {
        return "Events_" + hubProperties.getAppEnv() + "_" + contentOutput.getChannel() + "_" + random;
    }

    public void stop() {
        HubUtils.closeQuietly(contentOutput);
        webhookService.delete(getGroupName());
    }

}
