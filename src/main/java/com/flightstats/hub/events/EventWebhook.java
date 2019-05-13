package com.flightstats.hub.events;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;

class EventWebhook {

    private final String random = StringUtils.randomAlphaNumeric(6);

    private final ContentOutput contentOutput;
    private final WebhookService webhookService;
    private final String appUrl;
    private final String appEnv;

    EventWebhook(ContentOutput contentOutput,
                 WebhookService webhookService,
                 String appUrl,
                 String appEnv) {
        this.contentOutput = contentOutput;
        this.webhookService = webhookService;
        this.appUrl = appUrl;
        this.appEnv = appEnv;
    }

    public void start() {
        Webhook.WebhookBuilder builder = Webhook.builder()
                .name(getGroupName())
                .callbackUrl(getCallbackUrl())
                .channelUrl(getChannelUrl())
                .heartbeat(true)
                .startingKey(contentOutput.getContentKey())
                .batch(Webhook.SECOND);/**/
        Webhook webhook = builder.build();
        webhookService.upsert(webhook);
    }

    ContentOutput getContentOutput() {
        return contentOutput;
    }

    private String getChannelUrl() {
        return appUrl + "channel/" + contentOutput.getChannel();
    }

    private String getCallbackUrl() {
        return HubHost.getLocalHttpIpUri() + "/internal/events/" + getGroupName();
    }

    public String getGroupName() {
        return "Events_" + appEnv + "_" + contentOutput.getChannel() + "_" + random;
    }

    public void stop() {
        HubUtils.closeQuietly(contentOutput);
        webhookService.delete(getGroupName());
    }

}
