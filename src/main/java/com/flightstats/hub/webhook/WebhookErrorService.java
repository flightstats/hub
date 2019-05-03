package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.dao.ChannelService;
import com.flightstats.hub.model.Content;
import com.flightstats.hub.util.RequestUtils;
import com.flightstats.hub.webhook.error.WebhookError;
import com.flightstats.hub.webhook.error.WebhookErrorPruner;
import com.flightstats.hub.webhook.error.WebhookErrorRepository;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;

import static com.flightstats.hub.util.RequestUtils.getChannelName;
import static java.util.stream.Collectors.toList;

/**
 * This class is responsible for creation and management of webhook error strings.
 * <p>
 * Format: "{timestamp} {contentKey} {message}"
 * <p>
 * Example: "2018-01-02T03:04:05.006Z 2018/01/02/03/04/05/006/abcdef 400 Bad Request"
 * - timestamp: 2018-01-02T03:04:05.006Z
 * - contentKey: 2018/01/02/03/04/05/006/abcdef
 * - message: 400 Bad Request
 */

@Singleton
@Slf4j
class WebhookErrorService {

    private final WebhookErrorRepository webhookErrorRepository;
    private final ChannelService channelService;
    private final WebhookErrorPruner webhookErrorPruner;

    @Inject
    public WebhookErrorService(WebhookErrorRepository webhookErrorRepository,
                               WebhookErrorPruner webhookErrorPruner,
                               ChannelService channelService) {
        this.webhookErrorRepository = webhookErrorRepository;
        this.webhookErrorPruner = webhookErrorPruner;
        this.channelService = channelService;
    }

    public void add(String webhook, String error) {
        webhookErrorRepository.add(webhook, error);
        trimAndLookup(webhook);
    }

    public void delete(String webhook) {
        log.info("deleting webhook errors for " + webhook);
        webhookErrorRepository.deleteWebhook(webhook);
    }

    List<String> lookup(String webhook) {
        return trimAndLookup(webhook).stream()
                .map(WebhookError::getData)
                .collect(toList());
    }

    private List<WebhookError> trimAndLookup(String webhook) {
        final List<WebhookError> errors = webhookErrorRepository.getErrors(webhook).stream()
                .sorted(Comparator.comparing(WebhookError::getCreationTime))
                .collect(toList());

        final List<WebhookError> prunedErrors = webhookErrorPruner.pruneErrors(webhook, errors);

        return errors.stream()
                .filter(error -> !prunedErrors.contains(error))
                .collect(toList());
    }

    void publishToErrorChannel(DeliveryAttempt attempt) {
        if (attempt.getWebhook().getErrorChannelUrl() == null) return;

        final List<String> errors = lookup(attempt.getWebhook().getName());
        if (errors.size() < 1) {
            log.debug("no errors found for", attempt.getWebhook().getName());
            return;
        }

        final String error = errors.get(errors.size() - 1);
        byte[] bytes = buildPayload(attempt, error);
        final Content content = Content.builder()
                .withContentType("application/json")
                .withContentLength((long) bytes.length)
                .withData(bytes)
                .build();

        try {
            channelService.insert(getChannelName(attempt.getWebhook().getErrorChannelUrl()), content);
        } catch (Exception e) {
            log.warn("unable to publish errors for " + attempt.getWebhook().getName(), e);
        }
    }

    private byte[] buildPayload(DeliveryAttempt attempt, String error) {
        final ObjectMapper objectMapper = new ObjectMapper();
        final ObjectNode root = objectMapper.createObjectNode();
        root.put("webhookUrl", buildWebhookUrl(attempt));
        root.put("failedItemUrl", attempt.getWebhook().getChannelUrl() + "/" + attempt.getContentPath().toUrl());
        root.put("callbackUrl", attempt.getWebhook().getCallbackUrl());
        root.put("numberOfAttempts", attempt.getNumber() - 1);
        root.put("lastAttemptTime", extractTimestamp(error));
        root.put("lastAttemptError", extractMessage(error));
        return root.toString().getBytes();
    }

    private String buildWebhookUrl(DeliveryAttempt attempt) {
        // todo - workaround for HubHost.getLocalNamePort and AppProperties.getAppUrl not returning usable URLs for dockerized single hub
        final String host = RequestUtils.getHost(attempt.getWebhook().getChannelUrl());
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
