package com.flightstats.hub.webhook;

import com.flightstats.hub.cluster.Leadership;
import com.flightstats.hub.metrics.DataDog;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Predicate;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.timgroup.statsd.StatsDClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.concurrent.TimeUnit;

class WebhookRetryer {

    private final static Logger logger = LoggerFactory.getLogger(WebhookRetryer.class);
    private final static StatsDClient statsd = DataDog.statsd;

    static Retryer<ClientResponse> buildRetryer(Webhook webhook, WebhookError webhookError, final Leadership leadership) {

        return RetryerBuilder.<ClientResponse>newBuilder()
                .retryIfException(throwable -> {
                    if (throwable != null) {
                        if (throwable.getClass().isAssignableFrom(ClientHandlerException.class)) {
                            logger.info("got ClientHandlerException trying to call client back " + throwable.getMessage());
                        } else {
                            logger.info("got throwable trying to call client back ", throwable);
                        }
                        if (throwable instanceof ItemExpiredException) {
                            webhookError.publish(webhook);
                            return false;
                        }
                    }
                    emitErrorToDataDog(webhook.getName(), 500);
                    return throwable != null;
                })
                .retryIfResult(new Predicate<ClientResponse>() {
                    @Override
                    public boolean apply(@Nullable ClientResponse response) {
                        if (response == null) return true;
                        try {
                            boolean failure = response.getStatus() >= 400;
                            if (failure) {
                                emitErrorToDataDog(webhook.getName(), response.getStatus());
                                logger.info("unable to send to " + response);
                            }
                            return failure;
                        } finally {
                            close(response);
                        }
                    }

                    private void close(ClientResponse response) {
                        try {
                            response.close();
                        } catch (ClientHandlerException e) {
                            logger.info("exception closing response", e);
                        }
                    }
                })
                .withWaitStrategy(WaitStrategies.exponentialWait(1000, webhook.getMaxWaitMinutes(), TimeUnit.MINUTES))
                .withStopStrategy(failedAttempt -> !leadership.hasLeadership() || webhook.isPaused())
                .build();
    }

    private static void emitErrorToDataDog(String name, int status) {
        String[] tags = {"name:" + name, "status:" + status,};
        statsd.incrementCounter("webhook.errors", tags);
    }
}
