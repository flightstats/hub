package com.flightstats.hub.system;

import com.flightstats.hub.model.Webhook;
import com.flightstats.hub.model.WebhookType;
import com.flightstats.hub.system.service.CallbackService;
import com.flightstats.hub.system.service.ChannelConfigService;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

import static com.flightstats.hub.model.ChannelType.SINGLE;


@Slf4j
public class ModelBuilder {
    private final ChannelConfigService channelConfigService;
    private final CallbackService callbackService;

    @Inject
    public ModelBuilder(ChannelConfigService channelConfigService,
                        CallbackService callbackService) {
        this.channelConfigService = channelConfigService;
        this.callbackService = callbackService;
    }

    public WebhookBuilder webhookBuilder() {
        return new WebhookBuilder(channelConfigService, callbackService);
    }


    public static class WebhookBuilder {
        ChannelConfigService channelConfigService;
        CallbackService callbackService;
        Webhook.WebhookBuilder webhookBuilder;

        WebhookBuilder(ChannelConfigService channelConfigService, CallbackService callbackService) {
            this.channelConfigService = channelConfigService;
            this.callbackService = callbackService;
            this.webhookBuilder = Webhook.builder()
                    .batch(SINGLE.toString());
        }

        public WebhookBuilder webhookName(String webhookName) {
            this.webhookBuilder
                    .name(webhookName)
                    .callbackUrl(callbackService.getCallbackUrl(webhookName));
            return this;
        }

        public WebhookBuilder channelName(String channelName) {
            webhookBuilder
                    .channelUrl(channelConfigService.getChannelUrl(channelName));
            return this;
        }

        public WebhookBuilder parallelCalls(int calls) {
            webhookBuilder
                    .parallelCalls(calls);
            return this;
        }

        public WebhookBuilder batchType(WebhookType type) {
            webhookBuilder.batch(type.toString());
            return this;
        }

        public WebhookBuilder heartbeat(boolean value) {
            webhookBuilder.heartbeat(value);
            return this;
        }

        public WebhookBuilder callbackTimeoutSeconds(int seconds) {
            webhookBuilder.callbackTimeoutSeconds(seconds);
            return this;
        }


        public Webhook build() {
            return webhookBuilder.build();
        }


    }
}
