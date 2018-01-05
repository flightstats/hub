package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.google.inject.Inject;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

@NoArgsConstructor
class WebhookCarrier {

    private final static Logger logger = LoggerFactory.getLogger(WebhookLeader.class);
    private final static int CONNECT_TIMEOUT_SECONDS = 60;

    private Client httpClient;

    @Inject
    private WebhookError webhookError;

    private List<Predicate<DeliveryAttempt>> stopBeforeIfs = new ArrayList<>();
    private List<Predicate<DeliveryAttempt>> stopAfterIfs = new ArrayList<>();

    @Builder
    WebhookCarrier(@Singular List<Predicate<DeliveryAttempt>> stopBeforeIfs,
                   @Singular List<Predicate<DeliveryAttempt>> stopAfterIfs,
                   int timeoutSeconds) {
        this.stopBeforeIfs = stopBeforeIfs;
        this.stopAfterIfs = stopAfterIfs;
        this.httpClient = RestClient.createClient(CONNECT_TIMEOUT_SECONDS, timeoutSeconds, true, false);
    }

    void send(Webhook webhook, ContentPath contentPath, ObjectNode body) {
        int attemptNumber = 0;
        boolean isRetrying = true;
        while (isRetrying) {

            DeliveryAttempt attempt = DeliveryAttempt.builder()
                    .number(++attemptNumber)
                    .webhook(webhook)
                    .contentPath(contentPath)
                    .payload(body.toString())
                    .build();

            if (shouldStopBefore(attempt)) {
                logger.debug("{} {} stopping delivery before attempt #{}", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), attempt.getNumber());
                isRetrying = false;
                continue;
            }

            String payload = body.toString();
            logger.debug("{} {} delivery attempt #{} {} {}", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), attempt.getNumber(), webhook.getCallbackUrl(), payload);
            ClientResponse response = null;
            try {
                response = httpClient.resource(attempt.getWebhook().getCallbackUrl())
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .post(ClientResponse.class, payload);
                attempt.setStatusCode(response.getStatus());
            } catch (ClientHandlerException e) {
                logger.debug(webhook.getName() + " " + contentPath + " " + e.getMessage(), e);
                webhookError.add(webhook.getName(), new DateTime() + " " + contentPath + " " + e.getMessage());
                isRetrying = false;
            } finally {
                HubUtils.close(response);
            }

            if (shouldStopAfter(attempt)) {
                logger.debug("{} {} stopping delivery after attempt #{}", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), attempt.getNumber());
                isRetrying = false;
                continue;
            }

            try {
                long exponentialMultiplier = 1000;
                long maximumSleepTimeMS = TimeUnit.MINUTES.toMillis(attempt.getWebhook().getMaxWaitMinutes());
                long sleepTimeMS = calculateSleepTimeMS(attempt, exponentialMultiplier, maximumSleepTimeMS);
                logger.debug("{} {} waiting {} seconds until retrying", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), TimeUnit.MILLISECONDS.toSeconds(sleepTimeMS));
                Thread.sleep(sleepTimeMS);
            } catch (InterruptedException e) {
                String message = String.format("%s %s delivery halted due to interruption", attempt.getWebhook().getName(), attempt.getContentPath().toUrl());
                logger.debug(message, e);
                Thread.currentThread().interrupt();
                isRetrying = false;
            }
        }
    }

    private boolean shouldStopBefore(DeliveryAttempt attempt) {
        long stopBeforeCount = stopBeforeIfs.stream().filter(predicate -> predicate.test(attempt)).count();
        return stopBeforeCount != 0;
    }

    private boolean shouldStopAfter(DeliveryAttempt attempt) {
        long stopAfterCount = stopAfterIfs.stream().filter(predicate -> predicate.test(attempt)).count();
        return stopAfterCount != 0;
    }

    private long calculateSleepTimeMS(DeliveryAttempt attempt, long multiplier, long maximumSleepTimeMS) {
        double exp = Math.pow(2, attempt.getNumber());
        long exponentialSleepTimeMS = Math.round(multiplier * exp);
        return exponentialSleepTimeMS < maximumSleepTimeMS ? exponentialSleepTimeMS : maximumSleepTimeMS;
    }
}
