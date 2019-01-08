package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.util.RequestUtils;
import com.flightstats.hub.webhook.error.WebhookErrorPruner;
import com.flightstats.hub.webhook.error.WebhookErrorService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.flightstats.hub.util.RequestUtils.getChannelName;
import static java.util.stream.Collectors.toList;

/**
 * This class is responsible for creation and management of webhook error strings.
 *
 * Format: "{timestamp} {contentKey} {message}"
 *
 * Example: "2018-01-02T03:04:05.006Z 2018/01/02/03/04/05/006/abcdef 400 Bad Request"
 *  - timestamp: 2018-01-02T03:04:05.006Z
 *  - contentKey: 2018/01/02/03/04/05/006/abcdef
 *  - message: 400 Bad Request
 */

@Singleton
class WebhookError {
    private final static Logger logger = LoggerFactory.getLogger(WebhookError.class);

    private final WebhookErrorService webhookErrorService;
    private final ChannelService channelService;
    private final WebhookErrorPruner webhookErrorPruner;

    @Inject
    public WebhookError(WebhookErrorService webhookErrorService, WebhookErrorPruner webhookErrorPruner, ChannelService channelService) {
        this.webhookErrorService = webhookErrorService;
        this.webhookErrorPruner = webhookErrorPruner;
        this.channelService = channelService;
    }

    public void add(String webhook, String error) {
        webhookErrorService.add(webhook, error);
        limitChildren(webhook);
    }

    public void delete(String webhook) {
        logger.info("deleting webhook errors for " + webhook);
        webhookErrorService.deleteWebhook(webhook);
    }

    public List<String> get(String webhook) {
        return limitChildren(webhook).stream()
                .map(com.flightstats.hub.webhook.error.WebhookError::getData)
                .collect(toList());
    }

    private List<com.flightstats.hub.webhook.error.WebhookError> limitChildren(String webhook) {
        List<com.flightstats.hub.webhook.error.WebhookError> errors = webhookErrorService.getErrors(webhook);

        List<com.flightstats.hub.webhook.error.WebhookError> prunedErrors = webhookErrorPruner.pruneErrors(webhook, errors);

        return errors.stream()
                .filter(error -> !prunedErrors.contains(error))
                .collect(toList());
    }

    void publishToErrorChannel(DeliveryAttempt attempt) {
        if (attempt.getWebhook().getErrorChannelUrl() == null) return;

        List<String> errors = get(attempt.getWebhook().getName());
        if (errors.size() < 1) {
            logger.debug("no errors found for", attempt.getWebhook().getName());
            return;
        }

        String error = errors.get(errors.size() - 1);
        byte[] bytes = buildPayload(attempt, error);
        Content content = Content.builder()
                .withContentType("application/json")
                .withContentLength((long) bytes.length)
                .withData(bytes)
                .build();

        try {
            channelService.insert(getChannelName(attempt.getWebhook().getErrorChannelUrl()), content);
        } catch (Exception e) {
            logger.warn("unable to publish errors for " + attempt.getWebhook().getName(), e);
        }
    }

    private byte[] buildPayload(DeliveryAttempt attempt, String error) {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode root = objectMapper.createObjectNode();
        root.put("webhookUrl", buildWebhookUrl(attempt));
        root.put("failedItemUrl", attempt.getWebhook().getChannelUrl() + "/" + attempt.getContentPath().toUrl());
        root.put("callbackUrl", attempt.getWebhook().getCallbackUrl());
        root.put("numberOfAttempts", attempt.getNumber() - 1);
        root.put("lastAttemptTime", extractTimestamp(error));
        root.put("lastAttemptError", extractMessage(error));
        return root.toString().getBytes();
    }

    private String buildWebhookUrl(DeliveryAttempt attempt) {
        // todo - workaround for HubHost.getLocalNamePort and HubProperties.getAppUrl not returning usable URLs for dockerized single hub
        String host = RequestUtils.getHost(attempt.getWebhook().getChannelUrl());
        return host + "/webhook/" + attempt.getWebhook().getName();
    }

    private String extractTimestamp(String error) {
        return error.substring(0, error.indexOf(" "));
    }

    private String extractMessage(String error) {
        int firstSpace = error.indexOf(" ");
        int secondSpace = error.indexOf(" ", firstSpace + 1);
        return error.substring(secondSpace + 1);
    }
}
