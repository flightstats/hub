package com.flightstats.hub.events;

import com.flightstats.hub.config.properties.LocalHostProperties;
import com.flightstats.hub.util.HubUtils;
import com.flightstats.hub.util.StringUtils;
import com.flightstats.hub.webhook.Webhook;
import com.flightstats.hub.webhook.WebhookService;

import static com.flightstats.hub.model.WebhookType.SECOND;

class EventWebhook {

    private final String random = StringUtils.randomAlphaNumeric(6);

    private final ContentOutput contentOutput;
    private final WebhookService webhookService;
    private LocalHostProperties localHostProperties;
    private final String appUrl;
    private final String appEnv;

    EventWebhook(ContentOutput contentOutput,
                 WebhookService webhookService,
                 LocalHostProperties localHostProperties,
                 String appUrl,
                 String appEnv) {
        this.contentOutput = contentOutput;
        this.webhookService = webhookService;
        this.localHostProperties =localHostProperties;
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
                .batch(SECOND.name());/**/
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
        return localHostProperties.getUriWithHostIp() + "/internal/events/" + getGroupName();
    }

    String getGroupName() {
        return "Events_" + appEnv + "_" + contentOutput.getChannel() + "_" + random;
    }

    public void stop() {
        HubUtils.closeQuietly(contentOutput);
        webhookService.delete(getGroupName());
    }

}
