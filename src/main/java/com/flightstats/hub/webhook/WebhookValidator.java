package com.flightstats.hub.webhook;

import com.flightstats.hub.channel.ChannelValidator;
import com.flightstats.hub.exception.InvalidRequestException;
import com.flightstats.hub.util.RequestUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

public class WebhookValidator {

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
        if (!RequestUtils.isValidChannelUrl(webhook.getChannelUrl())) {
            throw new InvalidRequestException("{\"error\": \"Invalid channelUrl\"}");
        }
        webhook = webhook.withBatch(StringUtils.upperCase(webhook.getBatch()));
        if (Webhook.MINUTE.equals(webhook.getBatch())
                || Webhook.SECOND.equals(webhook.getBatch())
                || Webhook.SINGLE.equals(webhook.getBatch())) {
            return;
        } else {
            throw new InvalidRequestException("{\"error\": \"Allowed values for batch are 'SINGLE', 'SECOND' and 'MINUTE'\"}");
        }
    }
}
