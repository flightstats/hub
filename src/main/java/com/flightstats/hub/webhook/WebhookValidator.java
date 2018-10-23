package com.flightstats.hub.webhook;

import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;

import javax.inject.Inject;
import java.net.URI;
import java.net.URISyntaxException;

public class WebhookValidator {

    private final HubProperties hubProperties;

    @Inject
    WebhookValidator(HubProperties hubProperties) {
        this.hubProperties = hubProperties;
    }

    void validate(Webhook webhook) {
        String name = webhook.getName();
        if (StringUtils.isEmpty(name)) {
            throw new InvalidRequestException("{\"error\": \"Webhook name is required\"}");
        }
        if (!name.matches(ChannelValidator.VALID_NAME)) {
            throw new InvalidRequestException("{\"error\": \"Webhook name can only contain characters a-z, A-Z, _ and 0-9\"}");
        }
        if (name.length() > 128) {
            throw new InvalidRequestException("{\"error\": \"Webhook name must be less than 128 bytes\"}");
        }
        if (webhook.getParallelCalls() <= 0) {
            throw new InvalidRequestException("{\"error\": \"Webhook parallelCalls must be greater than zero\"}");
        }
        try {
            new URI(webhook.getCallbackUrl());
        } catch (URISyntaxException e) {
            throw new InvalidRequestException("{\"error\": \"Invalid callbackUrl\"}");
        }
        if (StringUtils.isEmpty(webhook.getTagUrl())) {
            if (!RequestUtils.isValidChannelUrl(webhook.getChannelUrl())) {
                throw new InvalidRequestException("{\"error\": \"Invalid channelUrl\"}");
            }
        } else { // if tag is not empty, the webhook config will be treated as a prototype - thus channel URL will be ignored
            if (!RequestUtils.isValidTagUrl(webhook.getTagUrl())) {
                throw new InvalidRequestException("{\"error\": \"Invalid tagUrl\"}");
            }
            if (!StringUtils.isEmpty(webhook.getChannelUrl())) {
                throw new InvalidRequestException("{\"error\": \"Either tagUrl or channelUrl should be defined, but not both\"}");
            }
        }
        webhook = webhook.withBatch(StringUtils.upperCase(webhook.getBatch()));
        if (!Webhook.MINUTE.equals(webhook.getBatch())
                && !Webhook.SECOND.equals(webhook.getBatch())
                && !Webhook.SINGLE.equals(webhook.getBatch())) {
            throw new InvalidRequestException("{\"error\": \"Allowed values for batch are 'SINGLE', 'SECOND' and 'MINUTE'\"}");
        }
        if (webhook.isHeartbeat() && Webhook.SINGLE.equals(webhook.getBatch())) {
            throw new InvalidRequestException("{\"error\": \"SINGLE webhooks can not have a heartbeat'\"}");
        }
        isValidCallbackTimeoutSeconds(webhook.getCallbackTimeoutSeconds());
        if (hubProperties.getProperty("hub.type", "aws").equals("aws")) {
            if (webhook.getCallbackUrl().toLowerCase().contains("localhost")) {
                throw new InvalidRequestException("{\"error\": \"A callbackUrl to localhost will never succeed.\"}");
            }
        }
    }

    private void isValidCallbackTimeoutSeconds(int value) {
        int minimum = hubProperties.getCallbackTimeoutMin();
        int maximum = hubProperties.getCallbackTimeoutMax();
        if (isOutsideRange(value, minimum, maximum)) {
            throw new InvalidRequestException("callbackTimeoutSeconds must be between " + minimum + " and " + maximum);
        }
    }

    private boolean isOutsideRange(int value, int minimum, int maximum) {
        return (value > maximum || value < minimum);
    }
}
