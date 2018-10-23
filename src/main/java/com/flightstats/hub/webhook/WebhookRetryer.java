package com.flightstats.hub.webhook;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.flightstats.hub.app.HubHost;
import com.flightstats.hub.app.HubProperties;
import com.flightstats.hub.metrics.ActiveTraces;
import com.flightstats.hub.metrics.MetricsService;
import com.flightstats.hub.metrics.Traces;
import com.flightstats.hub.model.ContentPath;
import com.flightstats.hub.model.RecurringTrace;
import com.flightstats.hub.rest.RestClient;
import com.flightstats.hub.util.HubUtils;
import com.google.common.annotations.VisibleForTesting;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

/**
 *  This class is responsible for trying to deliver a payload
 *  until a set of configurable criteria is met.
 */
class WebhookRetryer {

    private final static Logger logger = LoggerFactory.getLogger(WebhookRetryer.class);

    private final WebhookError webhookError;
    private final MetricsService metricsService;
    private final int defaultConnectTimeoutSeconds;
    private final int defaultReadTimeoutSeconds;

    private Client httpClient;
    private WebhookRetryerCriteria criteria;

    @Inject
    WebhookRetryer(WebhookError webhookError,  MetricsService metricsService, HubProperties hubProperties) {
        this.webhookError = webhookError;
        this.metricsService = metricsService;
        this.defaultConnectTimeoutSeconds = hubProperties.getProperty("webhook.connectTimeoutSeconds", 60);
        this.defaultReadTimeoutSeconds = hubProperties.getProperty("webhook.readTimeoutSeconds", 60);
        setCriteria(WebhookRetryerCriteria.builder().build());
    }

    void setCriteria(WebhookRetryerCriteria criteria) {
        this.criteria = criteria;
        int connectTimeoutSeconds = criteria.getConnectTimeoutSeconds() != null ? criteria.getConnectTimeoutSeconds() : defaultConnectTimeoutSeconds;
        int readTimeoutSeconds = criteria.getReadTimeoutSeconds() != null ? criteria.getReadTimeoutSeconds() : defaultReadTimeoutSeconds;
        initializeHttpClient(connectTimeoutSeconds, readTimeoutSeconds);
    }

    private void initializeHttpClient(int connectTimeoutSeconds, int readTimeoutSeconds) {
        this.httpClient = RestClient.createClient(connectTimeoutSeconds, readTimeoutSeconds, true, false);
    }

    boolean send(Webhook webhook, ContentPath contentPath, ObjectNode body) {
        Traces traces = ActiveTraces.getLocal();
        traces.add("WebhookRetryer.send start");
        RecurringTrace recurringTrace = new RecurringTrace("WebhookRetryer.send start");
        traces.add(recurringTrace);

        int attemptNumber = 0;
        boolean isDoneWithItem = false;
        boolean isRetrying = true;
        while (isRetrying) {

            DeliveryAttempt attempt = DeliveryAttempt.builder()
                    .number(++attemptNumber)
                    .webhook(webhook)
                    .contentPath(contentPath)
                    .payload(body.toString())
                    .build();

            boolean shouldGiveUp = shouldGiveUp(attempt);
            boolean shouldTryLater = shouldTryLater(attempt);

            if (shouldGiveUp || shouldTryLater) {
                logger.debug("{} {} stopping delivery before attempt #{}", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), attempt.getNumber());
                isRetrying = false;
                if (shouldGiveUp) {
                    isDoneWithItem = true;
                    webhookError.publishToErrorChannel(attempt);
                }

                continue;
            }

            String payload = body.toString();
            logger.debug("{} {} delivery attempt #{} {} {}", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), attempt.getNumber(), webhook.getCallbackUrl(), payload);
            ClientResponse response = null;
            try {
                response = httpClient.resource(attempt.getWebhook().getCallbackUrl())
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .header("Hub-Node", HubHost.getLocalNamePort())
                        .post(ClientResponse.class, payload);
                attempt.setStatusCode(response.getStatus());
            } catch (ClientHandlerException e) {
                attempt.setException(e);
            } finally {
                HubUtils.close(response);
            }

            String requestResult = determineResult(attempt);
            logger.debug("{} {} to {} response {}", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), attempt.getWebhook().getCallbackUrl(), requestResult);
            recurringTrace.update("WebhookLeader.send", "attempt " + attempt.getNumber(), ": " + requestResult);

            if (attempt.getStatusCode() != null && attempt.getStatusCode() < 400) {
                isRetrying = false;
                isDoneWithItem = true;
                continue;
            } else {
                webhookError.add(attempt.getWebhook().getName(), new DateTime() + " " + attempt.getContentPath() + " " + requestResult);
                metricsService.increment("webhook.errors", "name:" + attempt.getWebhook().getName(), "status:" + attempt.getStatusCode());
            }

            try {
                long exponentialMultiplier = 1000;
                long maximumSleepTimeMS = TimeUnit.MINUTES.toMillis(attempt.getWebhook().getMaxWaitMinutes());
                long sleepTimeMS = calculateSleepTimeMS(attempt, exponentialMultiplier, maximumSleepTimeMS);
                logger.debug("{} {} waiting {} seconds until retrying", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), TimeUnit.MILLISECONDS.toSeconds(sleepTimeMS));
                Thread.sleep(sleepTimeMS);
            } catch (InterruptedException e) {
                String message = String.format("%s %s to %s interrupted", attempt.getWebhook().getName(), attempt.getContentPath().toUrl(), attempt.getWebhook().getCallbackUrl());
                logger.debug(message, e);
                metricsService.increment("webhook.errors", "name:" + webhook.getName(), "status:500");
                Thread.currentThread().interrupt();
                isRetrying = false;
            }
        }

        recurringTrace.update("WebhookRetryer.send completed");
        return isDoneWithItem;
    }

    @VisibleForTesting
    boolean shouldGiveUp(DeliveryAttempt attempt) {
        long reasonsToGiveUp = criteria.getGiveUpIfs().stream().filter(predicate -> predicate.test(attempt)).count();
        return reasonsToGiveUp != 0;
    }

    @VisibleForTesting
    boolean shouldTryLater(DeliveryAttempt attempt) {
        long reasonsToTryLater = criteria.getTryLaterIfs().stream().filter(predicate -> predicate.test(attempt)).count();
        return reasonsToTryLater != 0;
    }

    @VisibleForTesting
    String determineResult(DeliveryAttempt attempt) {
        if (attempt.getException() == null) {
            return String.format("%s %s", attempt.getStatusCode(), Response.Status.fromStatusCode(attempt.getStatusCode()));
        } else {
            return attempt.getException().getMessage();
        }
    }

    @VisibleForTesting
    long calculateSleepTimeMS(DeliveryAttempt attempt, long multiplier, long maximumSleepTimeMS) {
        double result = Math.pow(2, attempt.getNumber());
        long exponentialSleepTimeMS = Math.round(multiplier * result);
        return Math.min(exponentialSleepTimeMS, maximumSleepTimeMS);
    }

}
