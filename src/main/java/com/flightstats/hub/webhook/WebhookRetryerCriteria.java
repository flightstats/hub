package com.flightstats.hub.webhook;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;
import java.util.function.Predicate;

@Value
class WebhookRetryerCriteria {

    private final List<Predicate<DeliveryAttempt>> giveUpIfs;
    private final List<Predicate<DeliveryAttempt>> tryLaterIfs;
    private final Integer connectTimeoutSeconds;
    private final Integer readTimeoutSeconds;

    @Builder
    WebhookRetryerCriteria(@Singular List<Predicate<DeliveryAttempt>> giveUpIfs,
                           @Singular List<Predicate<DeliveryAttempt>> tryLaterIfs,
                           Integer connectTimeoutSeconds,
                           Integer readTimeoutSeconds)
    {
        this.giveUpIfs = giveUpIfs;
        this.tryLaterIfs = tryLaterIfs;
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        this.readTimeoutSeconds = readTimeoutSeconds;
    }

}
