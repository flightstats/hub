package com.flightstats.hub.events;

import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.app.HubProvider;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;
import org.apache.commons.io.IOUtils;

class EventWebhook {

    private final WebhookService webhookService = HubProvider.getInstance(WebhookService.class);
    private final String random = StringUtils.randomAlphaNumeric(6);
    private ContentOutput contentOutput;

    EventWebhook(ContentOutput contentOutput) {
        this.contentOutput = contentOutput;
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
        return HubProperties.getAppUrl() + "channel/" + contentOutput.getChannel();
    }

    private String getCallbackUrl() {
        return HubHost.getLocalHttpIpUri() + "/internal/events/" + getGroupName();
    }

    public String getGroupName() {
        return "Events_" + HubProperties.getAppEnv() + "_" + contentOutput.getChannel() + "_" + random;
    }

    public void stop() {
        IOUtils.closeQuietly(contentOutput);
        webhookService.delete(getGroupName());
    }

}
